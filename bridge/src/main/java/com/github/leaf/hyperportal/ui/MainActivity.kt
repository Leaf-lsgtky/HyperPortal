// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.leaf.hyperportal.bigbang.TextSegmenter
import com.github.leaf.hyperportal.data.HyperPortalPreferences
import com.github.leaf.hyperportal.util.miuixBlurEffect
import com.github.leaf.hyperportal.util.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiuixTheme {
                HyperPortalMainScreen()
            }
        }
    }
}

@Composable
private fun HyperPortalMainScreen() {
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

    // Refresh state when resuming from BlacklistActivity
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
                triggerThreshold = config.triggerThreshold
                triggerPosition = config.triggerPosition
                blacklist = config.blacklist
                preloadTextSegmenter = config.preloadTextSegmenter
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val backdrop = rememberMiuixBlurBackdrop()
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "HyperPortal",
                scrollBehavior = scrollBehavior,
                modifier = Modifier
                    .miuixBlurEffect(backdrop)
                    .background(Color.Transparent),
                color = Color.Transparent,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = lazyListState,
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
            ),
        ) {
            // Group 1: 总开关
            item(key = "title_general") {
                SmallTitle(text = "全局设置")
            }
            item(key = "card_master_switch") {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
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
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
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
                        summary = "$touchSlop dp (长按时允许的手指微移阈值，超出则判定为滑动)",
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
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
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

            // End spacer per ui-guidelines.md
            item(key = "bottom_spacer") {
                Spacer(
                    modifier = Modifier
                        .height(24.dp)
                        .navigationBarsPadding(),
                )
            }
        }
    }
}
