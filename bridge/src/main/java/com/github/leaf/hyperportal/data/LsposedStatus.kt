// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.data

import android.content.Context
import io.github.libxposed.service.HookedTarget
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App 这一侧通往 LSPosed 的那条线（libxposed service, API 102）。
 *
 * 它给出两件首页要用的事：框架本身叫什么、什么版本（用来区分 LSPosed / 其他实现），以及
 * `runningTargets` —— 模块此刻被注入到了哪些进程里。后者是判断 system_server 与 SystemUI
 * 钩子是否就位的唯一可靠来源：那两个进程不归我们管，我们不能去问它们，只能问框架。
 *
 * 没有服务（框架较旧，或者还没绑上）时 `bound` 为 null，runningTargets 返回 null ——
 * 「问不到」和「一个都没有」必须分开，前者不该被画成红的。
 */
object LsposedStatus {

    private val boundFlow = MutableStateFlow<XposedService?>(null)

    /** 绑上的服务，页面据此在它到达时再问一次。 */
    val bound: StateFlow<XposedService?> = boundFlow

    @Volatile
    private var registered = false

    fun init(context: Context) {
        if (registered) return
        registered = true
        try {
            XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(service: XposedService) {
                    boundFlow.value = service
                }

                override fun onServiceDied(service: XposedService) {
                    if (boundFlow.value === service) boundFlow.value = null
                }
            })
        } catch (_: Throwable) {
        }
    }

    /** 模块此刻被注入的进程；null 表示没有服务可问。一次 binder 调用，不要在主线程上叫。 */
    fun runningTargets(): List<HookedTarget>? =
        boundFlow.value?.let { service ->
            runCatching { service.runningTargets }.getOrNull()
        }
}

/**
 * system_server 在这个列表里的样子：进程名是 `system`（Android 的内核进程名），uid 是 1000。
 * 两者都认，是因为不同框架版本报的字段不完全一致。
 */
fun HookedTarget.isSystemServer(): Boolean {
    val name = processName ?: ""
    return name == "system" || name == "android" || name == "system_server" || uid == 1000
}

fun HookedTarget.isSystemUi(): Boolean {
    val name = processName ?: ""
    return name == "com.android.systemui" || name.startsWith("com.android.systemui:")
}
