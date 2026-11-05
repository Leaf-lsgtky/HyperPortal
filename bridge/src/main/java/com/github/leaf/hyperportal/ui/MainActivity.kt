// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.leaf.hyperportal.util.miuixBlurEffect
import com.github.leaf.hyperportal.util.rememberMiuixBlurBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 外壳：两个 Tab，底部一条毛玻璃导航栏。
 *
 * 首页只说状态，设置页才是原来那唯一一页。分成两页的理由是它们回答的是两个不同的问题 ——
 * 「它现在好不好」和「我要改什么」—— 挤在一页里时，用户打开 App 先看到一堆开关，而真正
 * 想知道的那件事（模块到底有没有生效）要自己从开关的措辞里推断。
 */
class MainActivity : ComponentActivity() {

    /**
     * 每次回到前台加一，作为首页重新检查的 key。
     *
     * 模块可能在 App 待在后台的这段时间里被停用、被更新，或者某个被注入的进程重启了；一个
     * 挂在那里不动的页面会一直显示离开时的那份答案。
     */
    private var resumeEpoch by mutableStateOf(0)

    override fun onResume() {
        super.onResume()
        resumeEpoch++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiuixTheme {
                HyperPortalMainScreen(resumeKey = resumeEpoch)
            }
        }
    }
}

@Composable
private fun HyperPortalMainScreen(resumeKey: Int) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    var selectedIndex by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val items = listOf("首页", "设置")
    val icons = listOf(MiuixIcons.Home, MiuixIcons.Settings)

    // 手指滑过去时把底栏的选中态跟上去。点击切页时 selectedIndex 已经先设好了，这里不会打架。
    LaunchedEffect(pagerState.currentPage) {
        if (selectedIndex != pagerState.currentPage) {
            selectedIndex = pagerState.currentPage
        }
    }

    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberMiuixBlurBackdrop()

    fun goToPage(index: Int) {
        if (index == selectedIndex) return
        selectedIndex = index
        scope.launch { pagerState.animateScrollToPage(index) }
    }

    // 返回键先回到首页，再退才离开 App —— 与 KernelSU 的行为一致。
    BackHandler(enabled = selectedIndex != 0) { goToPage(0) }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .miuixBlurEffect(backdrop)
                    .background(Color.Transparent),
            ) {
                NavigationBar(color = Color.Transparent) {
                    items.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { goToPage(index) },
                            icon = icons[index],
                            label = label,
                        )
                    }
                }
            }
        },
    ) { globalPadding ->
        // 分页自己的 Scaffold 不持有 bottomBar，导航栏高度得从这里透传下去，否则最后一张
        // 卡片会被压在导航栏底下。
        val navBarHeight = globalPadding.calculateBottomPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(surfaceColor),
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                overscrollEffect = null,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> HomePage(bottomPadding = navBarHeight, resumeKey = resumeKey)
                    else -> SettingsPage(bottomPadding = navBarHeight)
                }
            }
        }
    }
}
