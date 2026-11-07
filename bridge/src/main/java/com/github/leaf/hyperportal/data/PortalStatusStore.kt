// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.data

import android.content.Context
import androidx.core.content.edit

/**
 * 「谁和 HyperPortal 说过话」的跨进程记录。
 *
 * 模块有三段住在别的进程里：system_server 的倾斜钩子、SystemUI 的悬浮层、以及被打过补丁的
 * AES 在每个目标 App 里的 Bridge。前两段能由 LSPosed 的 runningTargets 问到，第三段问不到
 * ——AES 不是 LSPosed 的作用域，它只是被改了 smali 的 ContentCatcher，只有它主动开口时
 * 我们才知道它还活着。
 *
 * 而它确实会开口：Bridge.ensureConfig() 每 5 秒最多调一次
 * `contentResolver.call("content://com.github.leaf.hyperportal.files", "getConfig", ...)`，
 * 那就是一次真实的握手 —— 请求落在 PortalFileProvider.call() 里，调用方 uid 就是目标 App。
 * 这里把每次握手的 uid 与时间记下来，首页据此说「AES 已握手 · 微信 · 刚刚」。
 *
 * 同样的思路也用在 SystemUI 上：PortalSystemUiModule.initOverlay() 会调
 * reportSystemUiActive，记一次时间戳，这样即使 LSPosed 服务没绑上，那一行也有话可说。
 *
 * 写入被节流：Bridge 在滑动时每 5 秒就可能来一次，每次都落盘毫无意义。
 */
object PortalStatusStore {

    const val PREFS_NAME = "hyperportal_status"

    private const val KEY_AES_ENTRIES = "aes_entries"
    private const val KEY_SYSTEMUI_AT = "systemui_at"

    /**
     * 记多少个不同的包。握手是按包去重的，这里只是防止无限增长。
     *
     * 比「首页要显示的个数」宽得多，是因为一次 ACTION_CONFIG_CHANGED 广播会让**每一个**装着
     * AES 桥的 App 同时来问配置（它们都注册了那个广播），一秒之内就能刷出十几条；上限设成刚
     * 好够显示，那一波就会把之前真正单独握过手的记录挤掉。
     */
    private const val MAX_AES_ENTRIES = 32

    /** 同一个包，一分钟内不重复落盘。 */
    private const val WRITE_THROTTLE_MS = 60_000L

    private const val SEP_FIELD = '|'
    private const val SEP_LINE = '\n'

    /**
     * getConfig 的调用方不止 AES 桥一个 —— SystemUI 与 system_server 的钩子也用它读配置
     * （PortalSystemUiModule.refreshConfig / PortalSystemModule），而它们各自已经有自己的
     * 状态来源。把它们算进「AES 握手」会让那一行永远显示「系统界面 · 刚刚」，反而盖住了真正
     * 想知道的事：有没有哪个第三方 App 里的桥和我们说过话。
     */
    private val IGNORED_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.github.leaf.hyperportal",
    )

    @Volatile
    private var lastWriteAt = 0L

    @Volatile
    private var lastPackage: String? = null

    /** 一次握手：谁、什么时候。 */
    data class Handshake(val packageName: String, val atMillis: Long)

    /**
     * 记一次 AES 握手。由 PortalFileProvider.call("getConfig") 调用，跑在 HyperPortal 自己的
     * 进程里（provider 与 App 同进程），所以这里可以随便用 SharedPreferences。
     */
    fun recordAesHandshake(context: Context?, uid: Int) {
        if (context == null) return
        if (uid == android.os.Process.SYSTEM_UID) return
        val pkg = packageForUid(context, uid) ?: return
        if (pkg in IGNORED_PACKAGES) return
        val now = System.currentTimeMillis()
        if (pkg == lastPackage && now - lastWriteAt < WRITE_THROTTLE_MS) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val entries = LinkedHashMap<String, Long>()
            for (h in readHandshakes(context)) entries[h.packageName] = h.atMillis
            entries.remove(pkg)
            entries[pkg] = now
            val trimmed = entries.entries
                .sortedByDescending { it.value }
                .take(MAX_AES_ENTRIES)
            val encoded = trimmed.joinToString(SEP_LINE.toString()) {
                it.key + SEP_FIELD + it.value
            }
            prefs.edit { putString(KEY_AES_ENTRIES, encoded) }
            lastPackage = pkg
            lastWriteAt = now
        } catch (_: Throwable) {
        }
    }

    /** 记一次 SystemUI 悬浮层的自报。由 reportSystemUiActive 调用。 */
    fun recordSystemUiActive(context: Context?) {
        if (context == null) return
        val now = System.currentTimeMillis()
        if (now - lastWriteAt < WRITE_THROTTLE_MS) return
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit { putLong(KEY_SYSTEMUI_AT, now) }
            lastWriteAt = now
        } catch (_: Throwable) {
        }
    }

    fun mergeFromSystemUi(context: Context?, systemUiAt: Long, rawAesEntries: String?) {
        if (context == null) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (systemUiAt > 0) {
                val current = prefs.getLong(KEY_SYSTEMUI_AT, 0L)
                if (systemUiAt > current) {
                    prefs.edit { putLong(KEY_SYSTEMUI_AT, systemUiAt) }
                }
            }
            if (!rawAesEntries.isNullOrBlank()) {
                val entries = LinkedHashMap<String, Long>()
                for (h in readHandshakes(context)) entries[h.packageName] = h.atMillis
                for (line in rawAesEntries.split(SEP_LINE)) {
                    val parts = line.split(SEP_FIELD)
                    if (parts.size >= 3) {
                        val pkg = parts[0]
                        val at = parts[2].toLongOrNull() ?: continue
                        if (pkg !in IGNORED_PACKAGES) {
                            val existing = entries[pkg] ?: 0L
                            if (at > existing) {
                                entries[pkg] = at
                            }
                        }
                    }
                }
                val trimmed = entries.entries
                    .sortedByDescending { it.value }
                    .take(MAX_AES_ENTRIES)
                val encoded = trimmed.joinToString(SEP_LINE.toString()) {
                    it.key + SEP_FIELD + it.value
                }
                prefs.edit { putString(KEY_AES_ENTRIES, encoded) }
            }
        } catch (_: Throwable) {
        }
    }

    /** 最近握过手的包，最新的在前。 */
    fun readHandshakes(context: Context?): List<Handshake> {
        if (context == null) return emptyList()
        return try {
            val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_AES_ENTRIES, null)
                ?: return emptyList()
            raw.split(SEP_LINE)
                .mapNotNull { line ->
                    val idx = line.lastIndexOf(SEP_FIELD)
                    if (idx <= 0) return@mapNotNull null
                    val pkg = line.substring(0, idx)
                    val at = line.substring(idx + 1).toLongOrNull() ?: return@mapNotNull null
                    // 也在这里挡一次：旧版本写进去的 SystemUI / system_server 条目还在盘上，
                    // 不清掉的话那一行会继续显示它们。
                    if (pkg.isEmpty() || pkg in IGNORED_PACKAGES) null else Handshake(pkg, at)
                }
                .sortedByDescending { it.atMillis }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /** SystemUI 最近一次自报的时间，0 表示从没报过。 */
    fun readSystemUiAt(context: Context?): Long {
        if (context == null) return 0L
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_SYSTEMUI_AT, 0L)
        } catch (_: Throwable) {
            0L
        }
    }

    private fun packageForUid(context: Context, uid: Int): String? = try {
        context.packageManager.getPackagesForUid(uid)?.firstOrNull()
    } catch (_: Throwable) {
        null
    }
}
