// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.leaf.hyperportal.util.miuixBlurEffect
import com.github.leaf.hyperportal.util.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 每个分页共用的页面骨架：毛玻璃顶栏 + 一条可以滚到顶栏底下的 LazyColumn。
 *
 * 抽出来是因为两个页面本来会各抄一遍，而抄出来的副本会各自漂移 —— 一页把顶距加在列表自己的
 * modifier 上而不是 contentPadding 里，列表就被压到顶栏下面，什么也滚不过去，毛玻璃没有东西
 * 可以采样，那一页的顶栏看起来就和另一页不一样。这个函数把这件事变成不可能，而不只是不常见。
 *
 * [bottomPadding] 是外层 MainPage 底部导航栏的高度。分页自己的 Scaffold 不持有 bottomBar，
 * 所以它的 innerPadding 里没有这一段；不传进来，最后一张卡片就会被导航栏压住。
 */
@Composable
fun PortalPageScaffold(
    title: String,
    bottomPadding: Dp,
    actions: @Composable (RowScope.() -> Unit) = {},
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberMiuixBlurBackdrop()
    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                scrollBehavior = scrollBehavior,
                modifier = Modifier
                    .miuixBlurEffect(backdrop)
                    .background(Color.Transparent),
                color = Color.Transparent,
                actions = actions,
            )
        },
        // 水平 insets 交给 contentPadding，竖向的交给自己 —— 顶栏自带状态栏内边距，底部由
        // bottomPadding 负责。这样 LazyColumn 才能滚到顶栏底下，毛玻璃才有内容可采样。
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding(),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = innerPadding.calculateBottomPadding() + bottomPadding,
            ),
            overscrollEffect = null,
            content = content,
        )
    }
}

/** 页面里每个块共用的间距：第一块 12 全周，之后每块左右 12、下方 12。 */
fun Modifier.firstBlock(): Modifier = padding(12.dp)

fun Modifier.nextBlock(): Modifier = padding(horizontal = 12.dp).padding(bottom = 12.dp)
