// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.data

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 首页要显示的全部状态。
 *
 * 三段钩子各自住在别的进程里，谁也不是从自己这里能直接看见的，所以每一项都来自一次真实的
 * 问询，而不是一个「应该没问题」的假设：
 *
 *  - [systemServerReady] / [systemUiReady]：LSPosed 的 runningTargets（见 [LsposedStatus]）
 *  - [aesHandshakes]：AES 的 Bridge 打到 SystemUI `__config__`（或回退到 PortalFileProvider）的握手（见 [PortalStatusStore]）
 *  - [masterSwitch]：本机配置，用户自己的总开关
 */
@Immutable
data class PortalStatus(
    /** 还没问出结果。页面据此显示「检查中」而不是先画一张红的。 */
    val checking: Boolean = true,
    /** LSPosed 服务绑上了没有。绑不上不代表钩子不在，只代表问不到。 */
    val serviceBound: Boolean = false,
    val frameworkName: String = "",
    val frameworkVersion: String = "",
    val systemServerReady: Boolean = false,
    val systemUiReady: Boolean = false,
    /** SystemUI 悬浮层最近一次自报的时间，0 表示没报过。 */
    val systemUiSeenAt: Long = 0L,
    val aesHandshakes: List<PortalStatusStore.Handshake> = emptyList(),
    val masterSwitch: Boolean = true,
) {
    /** 至少有一个 App 里的 AES 桥和我们握过手。 */
    val aesReady: Boolean get() = aesHandshakes.isNotEmpty()

    /** 两个 LSPosed 钩子都在。 */
    val hooksReady: Boolean get() = systemServerReady && systemUiReady

    /** 三段全通。 */
    val allReady: Boolean get() = hooksReady && aesReady

    /** 最近一次 AES 握手，没有则为 null。 */
    val lastHandshake: PortalStatusStore.Handshake? get() = aesHandshakes.firstOrNull()
}

/**
 * 把上面那些问询拼成一个 [PortalStatus]。
 *
 * 每次都是现问，不缓存到磁盘：钩子会随进程起落而变，上一次的答案对这一次没有意义。唯一
 * 留在内存里的 [last] 是为了让页面在重建时不必从「检查中」重新开始 —— 与 HyperMusicCover
 * 的 ModuleBridge.lastAnswer() 同一个理由。
 */
object PortalStatusRepository {

    @Volatile
    var last: PortalStatus? = null
        private set

    suspend fun probe(context: Context): PortalStatus = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        HyperPortalPreferences.syncToSystemUi(app)
        try {
            app.contentResolver.query(
                android.net.Uri.parse("content://com.android.systemui.fileprovider/hyperportal_incoming/__status__"),
                null, null, null, null
            )?.use { cursor ->
                val extras = cursor.extras
                if (extras != null) {
                    val sysUiAt = extras.getLong("systemui_at", 0L)
                    val rawAes = extras.getString("aes_entries", "")
                    PortalStatusStore.mergeFromSystemUi(app, sysUiAt, rawAes)
                }
            }
        } catch (_: Throwable) {
        }
        val service = LsposedStatus.bound.value
        val targets = LsposedStatus.runningTargets()
        val config = HyperPortalPreferences.getConfig(app)
        val systemUiSeenAt = PortalStatusStore.readSystemUiAt(app)
        val status = PortalStatus(
            checking = false,
            serviceBound = service != null,
            frameworkName = service?.frameworkName.orEmpty(),
            frameworkVersion = service?.frameworkVersion.orEmpty(),
            systemServerReady = targets?.any { it.isSystemServer() } == true,
            // 两种来源都认：框架说它在，或者它自己报过到。前者更准，后者在没有服务时兜底。
            systemUiReady = targets?.any { it.isSystemUi() } == true || systemUiSeenAt > 0L,
            systemUiSeenAt = systemUiSeenAt,
            aesHandshakes = PortalStatusStore.readHandshakes(app),
            masterSwitch = config.masterSwitch,
        )
        last = status
        status
    }
}
