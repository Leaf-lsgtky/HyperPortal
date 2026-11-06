// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ui

import android.content.Intent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.leaf.hyperportal.bigbang.TextSegmenter
import com.github.leaf.hyperportal.data.HyperPortalPreferences
import com.github.leaf.hyperportal.ui.component.PortalPageScaffold
import com.github.leaf.hyperportal.ui.component.nextBlock
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * 设置页：原来那个唯一的页面，原样搬过来。
 *
 * 它从「打开 App 看到的东西」退成「第二个 Tab」，但内容一条没动 —— 总开关、手势参数、手势
 * 排除都还是原来那几行，读写的也还是同一份 [HyperPortalPreferences]。
 */
@Composable
fun SettingsPage(bottomPadding: Dp) {
    val context = LocalContext.current
    var masterSwitch by remember {
        mutableStateOf(HyperPortalPreferences.getConfig(context).masterSwitch)
    }
    var landscapeTrigger by remember {
        mutableStateOf(HyperPortalPreferences.getConfig(context).landscapeTrigger)
    }
    var edgeScrollMargin by remember {
        mutableIntStateOf(HyperPortalPreferences.getConfig(context).edgeScrollMargin)
    }
    var longPressDuration by remember {
        mutableIntStateOf(HyperPortalPreferences.getConfig(context).longPressDuration)
    }
    var touchSlop by remember {
        mutableIntStateOf(HyperPortalPreferences.getConfig(context).touchSlop)
    }
    var dragStartSlop by remember {
        mutableIntStateOf(HyperPortalPreferences.getConfig(context).dragStartSlop)
    }
    var sameWindowGuard by remember {
        mutableStateOf(HyperPortalPreferences.getConfig(context).sameWindowGuard)
    }
    var triggerThreshold by remember {
        mutableIntStateOf(HyperPortalPreferences.getConfig(context).triggerThreshold)
    }
    var triggerPosition by remember {
        mutableStateOf(HyperPortalPreferences.getConfig(context).triggerPosition)
    }
    var blacklist by remember {
        mutableStateOf(HyperPortalPreferences.getConfig(context).blacklist)
    }
    var preloadTextSegmenter by remember {
        mutableStateOf(HyperPortalPreferences.getConfig(context).preloadTextSegmenter)
    }

    // Refresh state when resuming from BlacklistActivity / TargetSettingsActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val config = HyperPortalPreferences.getConfig(context)
                masterSwitch = config.masterSwitch
                landscapeTrigger = config.landscapeTrigger
                edgeScrollMargin = config.edgeScrollMargin
                longPressDuration = config.longPressDuration
                touchSlop = config.touchSlop
                dragStartSlop = config.dragStartSlop
                sameWindowGuard = config.sameWindowGuard
                triggerThreshold = config.triggerThreshold
                triggerPosition = config.triggerPosition
                blacklist = config.blacklist
                preloadTextSegmenter = config.preloadTextSegmenter
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PortalPageScaffold(title = "设置", bottomPadding = bottomPadding) {
        // Group 1: 总开关
        item(key = "title_general") {
            SmallTitle(text = "全局设置")
        }
        item(key = "card_master_switch") {
            Card(modifier = Modifier.nextBlock()) {
                SwitchPreference(
                    title = "功能总开关",
                    summary = if (masterSwitch) "HyperPortal 运行中（长按内容可触发传送门）" else "已暂停运行",
                    checked = masterSwitch,
                    onCheckedChange = { checked ->
                        masterSwitch = checked
                        HyperPortalPreferences.setMasterSwitch(context, checked)
                    },
                )
                ArrowPreference(
                    title = "拖拽目标与置顶",
                    summary = "管理分享入口、置顶功能、显示数量和排序",
                    enabled = masterSwitch,
                    onClick = {
                        context.startActivity(Intent(context, TargetSettingsActivity::class.java))
                    },
                )
                SwitchPreference(
                    title = "允许横屏触发",
                    summary = if (landscapeTrigger) "横屏模式下允许长按触发传送门" else "横屏模式下禁止长按触发（推荐）",
                    checked = landscapeTrigger,
                    enabled = masterSwitch,
                    onCheckedChange = { checked ->
                        landscapeTrigger = checked
                        HyperPortalPreferences.setLandscapeTrigger(context, checked)
                    },
                )
                SwitchPreference(
                    title = "同窗口拖拽不投递",
                    summary = if (sameWindowGuard) {
                        "拖回起拖窗口时不把 drop 交给应用（避免误拖的消息被直接发出），阴影回弹"
                    } else {
                        "交给应用自行处理拖回起拖窗口的 drop"
                    },
                    checked = sameWindowGuard,
                    enabled = masterSwitch,
                    onCheckedChange = { checked ->
                        sameWindowGuard = checked
                        HyperPortalPreferences.setSameWindowGuard(context, checked)
                    },
                )
                SwitchPreference(
                    title = "预加载分词库",
                    summary = if (preloadTextSegmenter) {
                        "启动时后台加载词典，缩短首次打开文本分词的等待"
                    } else {
                        "首次打开文本分词时再加载词典"
                    },
                    checked = preloadTextSegmenter,
                    enabled = masterSwitch,
                    onCheckedChange = { checked ->
                        preloadTextSegmenter = checked
                        HyperPortalPreferences.setPreloadTextSegmenter(context, checked)
                        if (checked) {
                            TextSegmenter.preload(context)
                        }
                    },
                )
            }
        }

        // Group 2: 手势参数
        item(key = "title_gesture") {
            SmallTitle(text = "手势参数")
        }
        item(key = "card_gesture_params") {
            Card(modifier = Modifier.nextBlock()) {
                SliderPreference(
                    title = "长按触发时间",
                    summary = "$longPressDuration 毫秒",
                    value = longPressDuration.toFloat(),
                    valueRange = 200f..1500f,
                    steps = 25, // 200..1500 step 50
                    enabled = masterSwitch,
                    onValueChange = { raw ->
                        val rounded = (Math.round(raw / 50f) * 50).coerceIn(200, 1500)
                        longPressDuration = rounded
                        HyperPortalPreferences.setLongPressDuration(context, rounded)
                    },
                )
                SliderPreference(
                    title = "防误触滑动容差",
                    summary = "$touchSlop dp (长按成立前允许的手指微移，超出即判定为滑动并放弃长按)",
                    value = touchSlop.toFloat(),
                    valueRange = 8f..48f,
                    steps = 19, // 8..48 step 2
                    enabled = masterSwitch,
                    onValueChange = { raw ->
                        val rounded = (Math.round(raw / 2f) * 2).coerceIn(8, 48)
                        touchSlop = rounded
                        HyperPortalPreferences.setTouchSlop(context, rounded)
                    },
                )
                SliderPreference(
                    title = "起拖位移阈值",
                    summary = "$dragStartSlop dp (长按出预览后，手指移动超过该距离才开始真正的拖拽)",
                    value = dragStartSlop.toFloat(),
                    valueRange = 4f..32f,
                    steps = 13, // 4..32 step 2
                    enabled = masterSwitch,
                    onValueChange = { raw ->
                        val rounded = (Math.round(raw / 2f) * 2).coerceIn(4, 32)
                        dragStartSlop = rounded
                        HyperPortalPreferences.setDragStartSlop(context, rounded)
                    },
                )
                SliderPreference(
                    title = "边缘滚动触发区域",
                    summary = "$edgeScrollMargin dp (手指拖至屏幕左右两侧此宽度内触发应用列表滚动)",
                    value = edgeScrollMargin.toFloat(),
                    valueRange = 40f..140f,
                    steps = 19, // 40..140 step 5
                    enabled = masterSwitch,
                    onValueChange = { raw ->
                        val rounded = (Math.round(raw / 5f) * 5).coerceIn(40, 140)
                        edgeScrollMargin = rounded
                        HyperPortalPreferences.setEdgeScrollMargin(context, rounded)
                    },
                )
                val positionItems = listOf("下侧（底部）", "右侧（边缘）")
                val selectedPositionIndex = if (triggerPosition == HyperPortalPreferences.POSITION_RIGHT) 1 else 0
                val isRight = triggerPosition == HyperPortalPreferences.POSITION_RIGHT

                OverlayDropdownPreference(
                    title = "触发位置",
                    summary = if (isRight) "手指拖至屏幕右侧边缘触发倾斜与应用列表" else "手指拖至屏幕底部触发倾斜与应用列表",
                    items = positionItems,
                    selectedIndex = selectedPositionIndex,
                    enabled = masterSwitch,
                    onSelectedIndexChange = { index ->
                        val newPos = if (index == 1) HyperPortalPreferences.POSITION_RIGHT else HyperPortalPreferences.POSITION_BOTTOM
                        triggerPosition = newPos
                        HyperPortalPreferences.setTriggerPosition(context, newPos)
                    },
                )
                SliderPreference(
                    title = if (isRight) "右侧触发阈值" else "下侧触发阈值",
                    summary = if (isRight) {
                        "$triggerThreshold% 屏幕宽度 (手指拖至屏幕右侧 $triggerThreshold% 时触发倾斜与应用列表)"
                    } else {
                        "$triggerThreshold% 屏幕高度 (手指拖至屏幕下方 $triggerThreshold% 时触发倾斜与应用列表)"
                    },
                    value = triggerThreshold.toFloat(),
                    valueRange = 50f..95f,
                    steps = 44, // 50%..95% step 1%
                    enabled = masterSwitch,
                    onValueChange = { raw ->
                        val rounded = Math.round(raw).coerceIn(50, 95)
                        triggerThreshold = rounded
                        HyperPortalPreferences.setTriggerThreshold(context, rounded)
                    },
                )
            }
        }

        // Group 3: 手势排除
        item(key = "title_blacklist") {
            SmallTitle(text = "手势排除")
        }
        item(key = "card_blacklist") {
            Card(modifier = Modifier.nextBlock()) {
                ArrowPreference(
                    title = "手势黑名单",
                    summary = if (blacklist.isEmpty()) {
                        "未排除任何应用"
                    } else {
                        "已在 ${blacklist.size} 个应用中禁用长按手势"
                    },
                    enabled = masterSwitch,
                    onClick = {
                        context.startActivity(Intent(context, BlacklistActivity::class.java))
                    },
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(24.dp))
        }
    }
}
