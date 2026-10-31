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
    var longPressDuration by remember {
        mutableIntStateOf(HyperPortalPreferences.getConfig(context).longPressDuration)
    }
    var touchSlop by remember {
        mutableIntStateOf(HyperPortalPreferences.getConfig(context).touchSlop)
    }
    var blacklist by remember {
        mutableStateOf(HyperPortalPreferences.getConfig(context).blacklist)
    }

    // Refresh state when resuming from BlacklistActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val config = HyperPortalPreferences.getConfig(context)
                masterSwitch = config.masterSwitch
                longPressDuration = config.longPressDuration
                touchSlop = config.touchSlop
                blacklist = config.blacklist
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
                        valueRange = 200f..800f,
                        steps = 11, // 200..800 step 50
                        enabled = masterSwitch,
                        onValueChange = { raw ->
                            val rounded = (Math.round(raw / 50f) * 50).coerceIn(200, 800)
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
