// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.leaf.hyperportal.data.LsposedStatus
import com.github.leaf.hyperportal.data.PortalStatus
import com.github.leaf.hyperportal.data.PortalStatusRepository
import com.github.leaf.hyperportal.ui.component.PortalPageScaffold
import com.github.leaf.hyperportal.ui.component.firstBlock
import com.github.leaf.hyperportal.ui.component.nextBlock
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 首页：只说状态，不放任何开关。
 *
 * 模块由三段各自住在别的进程里的钩子组成，用户看不见它们中的任何一个 —— 出了问题只能靠猜。
 * 这一页存在的意义就是把「它们现在各自是什么状态」摆到台面上：LSPosed 问得到的两个进程由框架
 * 回答，AES 那一段由它自己握手时报上来。
 *
 * 版式取自 KernelSU / HyperMusicCover / InstallerX / MiuiBackGestureHook 的首页状态卡：
 * 一张通栏大卡（标题与副标题左上、工作模式左下、超大图标从右下角出血），下面几张只有键值的
 * 信息卡。点大卡重新检查。
 */
@Composable
fun HomePage(bottomPadding: Dp, resumeKey: Int, isCurrent: () -> Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 从上一个答案开始，而不是从「检查中」开始：这一页每次切回来都会重建，若每次都先画一张
    // 「正在检查」的灰卡再换成结果，等于每次切换都要闪一下。上一个答案不会更错。
    var status by remember { mutableStateOf(PortalStatusRepository.last ?: PortalStatus()) }
    var checked by remember { mutableStateOf(PortalStatusRepository.last != null) }

    suspend fun refresh() {
        status = PortalStatusRepository.probe(context)
        checked = true
    }

    // 成为当前页时问一次，回到前台时再问一次 —— 后者包括「在设置页关掉总开关，切回首页」。
    // 两页都常驻，所以「切回来」不会重建，只有这个 key 能把新状态叫出来。
    val current = isCurrent()
    LaunchedEffect(current, resumeKey) { if (current) refresh() }

    // LSPosed 的服务是异步绑上来的，通常晚于第一帧。它一到就再问一次，否则首屏会停在
    // 「钩子未注入」——那不是答案，只是还没问到。
    val service by LsposedStatus.bound.collectAsStateWithLifecycle()
    LaunchedEffect(service) { if (service != null) refresh() }

    PortalPageScaffold(
        title = "HyperPortal",
        bottomPadding = bottomPadding,
        actions = {
            IconButton(onClick = { scope.launch { refresh() } }) {
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = "重新检查",
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            }
        },
    ) {
        item(key = "status") {
            PortalStatusCard(
                status = status,
                checked = checked,
                modifier = Modifier.firstBlock(),
                onRefresh = { scope.launch { refresh() } },
            )
        }

        item(key = "detail") {
            Card(modifier = Modifier.nextBlock()) {
                BasicComponent(
                    title = "功能总开关",
                    summary = if (status.masterSwitch) "运行中" else "已暂停",
                )
                BasicComponent(
                    title = "LSPosed 框架",
                    summary = frameworkSummary(status),
                )
                BasicComponent(
                    title = "system_server 钩子",
                    summary = systemServerSummary(status),
                )
                BasicComponent(
                    title = "系统界面钩子",
                    summary = systemUiSummary(status),
                )
                BasicComponent(
                    title = "AES 握手",
                    summary = aesSummary(context, status),
                )
            }
        }

        item(key = "environment") {
            Card(modifier = Modifier.nextBlock()) {
                BasicComponent(title = "设备型号", summary = deviceModel())
                BasicComponent(title = "系统版本", summary = hyperOsVersion())
                BasicComponent(
                    title = "Android 版本",
                    summary = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
                )
                BasicComponent(title = "模块版本", summary = moduleVersion(context))
            }
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * 通栏状态卡。
 *
 * 五个状态而不是两个。总开关关掉时钩子还在，说「已激活」是骗人；LSPosed 服务没绑上时钩子可能
 * 好好的，说「未激活」同样是骗人 —— 那是「问不到」，不是「不在」；钩子在、但还没有任何 App
 * 完成过 AES 握手，是开机之后的常态，用琥珀色说清楚「还差一步，去长按一个内容」；连钩子都
 * 确认没找到才是红色。
 */
@Composable
private fun PortalStatusCard(
    status: PortalStatus,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
) {
    val paused = checked && !status.masterSwitch
    val unknown = checked && !paused && !status.serviceBound
    val ready = checked && !paused && !unknown && status.allReady
    val hooksOnly = checked && !paused && !unknown && status.hooksReady && !status.aesReady
    val problem = checked && !paused && !unknown && !status.hooksReady
    val attention = paused || unknown || hooksOnly

    val dark = isSystemInDarkTheme()
    val dynamic = MiuixTheme.isDynamicColor

    val cardColor = when {
        ready && dynamic -> MiuixTheme.colorScheme.secondaryContainer
        ready && dark -> Color(0xFF1A3825)
        ready -> Color(0xFFDFFAE4)
        attention && dark -> Color(0xFF3D3215)
        attention -> Color(0xFFFFF3CD)
        problem && dynamic -> MiuixTheme.colorScheme.errorContainer
        problem && dark -> Color(0xFF3D1C1C)
        problem -> Color(0xFFFDE8E8)
        dynamic -> MiuixTheme.colorScheme.surfaceContainerHigh
        dark -> Color(0xFF232323)
        else -> Color(0xFFF2F2F2)
    }
    val iconTint = when {
        ready && dynamic -> MiuixTheme.colorScheme.primary.copy(alpha = 0.8f)
        ready -> Color(0xFF36D167)
        attention && dark -> Color(0xFFFFC107)
        attention -> Color(0xFFFFB300)
        problem && dynamic -> MiuixTheme.colorScheme.error.copy(alpha = 0.8f)
        problem -> Color(0xFFDC3545)
        else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    val icon = when {
        ready -> Icons.Rounded.CheckCircleOutline
        attention -> Icons.Rounded.WarningAmber
        problem -> Icons.Rounded.ErrorOutline
        else -> Icons.Rounded.HourglassEmpty
    }
    val title = when {
        paused -> "已暂停运行"
        unknown -> "无法确认状态"
        ready -> "HyperPortal 已激活"
        hooksOnly -> "等待 AES 握手"
        problem -> "HyperPortal 未激活"
        else -> "正在检查…"
    }
    val summary = when {
        paused -> "功能总开关已关闭，长按内容不会触发传送门"
        unknown -> "LSPosed 服务未连接，查不到钩子被注入到了哪些进程"
        ready -> "三段钩子全部就绪"
        hooksOnly -> "钩子已注入，长按任意应用里的内容即可完成握手"
        problem -> "LSPosed 中没有找到已注入的进程，请检查模块是否启用"
        else -> "正在询问 LSPosed 与 AES 桥"
    }
    // 卡片左下角那一行：与右下角的大图标斜对角平衡。没有可说的就不放。
    val mode = when {
        ready -> status.frameworkName.ifBlank { "LSPosed" }
        hooksOnly -> "等待握手"
        else -> null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(color = cardColor),
            onClick = onRefresh,
            showIndication = true,
            pressFeedbackType = PressFeedbackType.Tilt,
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(27.dp, 31.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Icon(
                        modifier = Modifier.size(110.dp),
                        imageVector = icon,
                        tint = iconTint,
                        contentDescription = null,
                    )
                }
                if (mode != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp, 10.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Text(
                            text = mode,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp, 14.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Column {
                        Text(
                            text = title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = summary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

private fun frameworkSummary(status: PortalStatus): String {
    if (!status.serviceBound) return "未连接，无法查询注入状态"
    val name = status.frameworkName.ifBlank { "LSPosed" }
    return if (status.frameworkVersion.isBlank()) name else "$name ${status.frameworkVersion}"
}

/**
 * system_server 只能由框架回答 —— 它不像 SystemUI 那样会自己报一声。服务没绑上时它是「不知道」，
 * 不是「没注入」，这两件事在页面上必须分开说。
 */
private fun systemServerSummary(status: PortalStatus): String = when {
    !status.serviceBound -> "未知（LSPosed 服务未连接）"
    status.systemServerReady -> "已注入"
    else -> "未注入"
}

private fun systemUiSummary(status: PortalStatus): String {
    if (!status.systemUiReady) return "未注入"
    val seen = relativeTime(status.systemUiSeenAt)
    return if (seen.isEmpty()) "已注入" else "已注入 · $seen"
}

private fun aesSummary(context: Context, status: PortalStatus): String {
    val last = status.lastHandshake ?: return "尚未握手"
    val label = appLabel(context, last.packageName)
    val when_ = relativeTime(last.atMillis)
    // 一次配置广播会让所有装着桥的 App 在同一毫秒里一起来问，所以「最近一个」经常只是那一
    // 波里的某一个。把个数一并说出来，这一行才不会看起来像在随机换名字。
    val count = status.aesHandshakes.size
    val who = if (count > 1) "$label 等 $count 个应用" else label
    return if (when_.isEmpty()) who else "$who · $when_"
}

/** 一个包名换成人看得懂的名字；查不到就原样显示包名。 */
private fun appLabel(context: Context, packageName: String): String = try {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
} catch (_: Throwable) {
    packageName
}

private fun relativeTime(millis: Long): String {
    if (millis <= 0L) return ""
    val delta = System.currentTimeMillis() - millis
    return when {
        delta < 60_000L -> "刚刚"
        delta < 3_600_000L -> "${delta / 60_000L} 分钟前"
        delta < 86_400_000L -> "${delta / 3_600_000L} 小时前"
        else -> "${delta / 86_400_000L} 天前"
    }
}

private fun deviceModel(): String = Build.MODEL.ifBlank { "未知" }

private fun moduleVersion(context: Context): String = try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    "v${info.versionName}"
} catch (_: Throwable) {
    "未知"
}

/**
 * HyperOS 的版本号只写在 ro.* 属性里。增量号本身就以发布名开头（OS4.0.0.35.XPBCNXM），
 * 前面再拼一次名字只会变成「OS4.0 · OS4.0.0.35.XPBCNXM」。
 */
private object SystemVersion {
    private fun prop(key: String): String = try {
        val cls = Class.forName("android.os.SystemProperties")
        val get = cls.getMethod("get", String::class.java, String::class.java)
        (get.invoke(null, key, "") as? String).orEmpty()
    } catch (_: Throwable) {
        ""
    }

    fun hyperOs(fallback: String): String {
        val incremental = prop("ro.mi.os.version.incremental").ifEmpty { Build.DISPLAY }
        if (incremental.isNotEmpty()) return incremental
        val name = prop("ro.mi.os.version.name").ifEmpty { prop("ro.miui.ui.version.name") }
        return name.ifEmpty { fallback }
    }
}

private fun hyperOsVersion(): String = SystemVersion.hyperOs("未知")
