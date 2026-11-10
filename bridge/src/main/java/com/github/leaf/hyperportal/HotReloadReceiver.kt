// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.github.leaf.hyperportal.data.LsposedStatus
import com.github.leaf.hyperportal.data.isSystemServer
import com.github.leaf.hyperportal.data.isSystemUi
import io.github.libxposed.service.HookedTarget
import io.github.libxposed.service.HotReloadResult
import io.github.libxposed.service.XposedService

/**
 * 开发辅助：请 libxposed（API 102）把本模块热重载进已经在跑的 system_server / SystemUI，
 * 这样改完代码不必重启就能验证。
 *
 * 关键事实（实测）：替换模块 APK 本身**不会**触发热重载，`autoHotReload=true` 只是允许被
 * 重载；真正发起请求的是 `XposedService.hotReloadModule(target, extras, callback)`，而且必须
 * 由一个「自己不是被注入目标」的进程来调 —— 也就是本 App。
 *
 *   adb shell am broadcast -a com.github.leaf.hyperportal.HOT_RELOAD \
 *       -n com.github.leaf.hyperportal/.HotReloadReceiver
 *
 * 结果打到 logcat 的 HyperPortalHotReload 标签：每个目标的 status 与 message。
 */
class HotReloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!isTrustedSender()) {
            Log.println(Log.WARN, TAG, "ignoring hot reload request from uid=$sentFromUid")
            return
        }
        val app = context.applicationContext
        val pending = goAsync()
        Thread({
            try {
                LsposedStatus.init(app)
                val service = awaitService(SERVICE_WAIT_MS)
                if (service == null) {
                    Log.println(Log.WARN, TAG, "libxposed service unavailable; nothing reloaded")
                    return@Thread
                }
                val targets = runCatching { service.runningTargets }
                    .getOrNull()
                    .orEmpty()
                    .filter { it.isSystemServer() || it.isSystemUi() }
                if (targets.isEmpty()) {
                    Log.println(Log.WARN, TAG, "no system_server/SystemUI target; nothing reloaded")
                    return@Thread
                }
                for (target in targets) {
                    Log.println(Log.INFO, TAG,
                        "requesting hot reload of ${target.processName} pid=${target.pid}")
                    runCatching {
                        service.hotReloadModule(target, Bundle(),
                            object : XposedService.HotReloadCallback {
                                override fun onHotReloadResult(
                                    reloaded: HookedTarget,
                                    result: HotReloadResult
                                ) {
                                    Log.println(Log.INFO, TAG,
                                        "hot reload ${reloaded.processName} -> "
                                                + "${result.status()} ${result.message()}")
                                }
                            })
                    }.onFailure {
                        Log.println(Log.WARN, TAG, "hot reload request failed: $it")
                    }
                }
            } catch (throwable: Throwable) {
                Log.println(Log.WARN, TAG, "hot reload failed: $throwable")
            } finally {
                pending.finish()
            }
        }, "HyperPortalHotReload").start()
    }

    /** adb shell（2000）、root（0）、system（1000）才允许触发；-1 表示框架没给出 uid（am broadcast）。 */
    private fun isTrustedSender(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val uid = sentFromUid
        return uid <= 0 || uid == 1000 || uid == 2000
    }

    private fun awaitService(timeoutMs: Long): XposedService? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            LsposedStatus.bound.value?.let { return it }
            try {
                Thread.sleep(50L)
            } catch (_: InterruptedException) {
                return null
            }
        }
        return LsposedStatus.bound.value
    }

    companion object {
        private const val TAG = "HyperPortalHotReload"
        private const val SERVICE_WAIT_MS = 5_000L
        const val ACTION = "com.github.leaf.hyperportal.HOT_RELOAD"
    }
}
