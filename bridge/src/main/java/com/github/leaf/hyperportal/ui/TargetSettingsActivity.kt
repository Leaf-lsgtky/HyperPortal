// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.github.leaf.hyperportal.data.HyperPortalPreferences
import com.github.leaf.hyperportal.target.ShareTargetEntry
import com.github.leaf.hyperportal.target.TargetRepository
import com.github.leaf.hyperportal.util.miuixBlurEffect
import com.github.leaf.hyperportal.util.rememberMiuixBlurBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

class TargetSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiuixTheme {
                TargetSettingsScreen(onClose = { finish() })
            }
        }
    }
}

/** Number of static LazyColumn items before the draggable pinned rows. */
private const val PINNED_START_INDEX = 5

private data class AppTargetGroup(
    val packageName: String,
    val appName: String,
    val targets: List<ShareTargetEntry>,
)

@Composable
private fun TargetSettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var config by remember { mutableStateOf(HyperPortalPreferences.getConfig(context)) }
    var allTargets by remember { mutableStateOf<List<ShareTargetEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var targetLimit by remember { mutableIntStateOf(config.targetLimit) }
    val pinnedItems = remember { mutableStateListOf<ShareTargetEntry>() }
    var expandedAppKeys by remember { mutableStateOf(emptySet<String>()) }
    val listState = rememberLazyListState()

    fun refreshConfigAndPinned() {
        config = HyperPortalPreferences.getConfig(context)
        val keyed = allTargets.associateBy { it.key }
        val ordered = config.pinnedTargetOrder.mapNotNull { keyed[it] }
            .filter { config.pinnedTargetKeys.contains(it.key) }
        val seen = ordered.map { it.key }.toMutableSet()
        val remaining = config.pinnedTargetKeys.mapNotNull { keyed[it] }
            .filterNot { seen.contains(it.key) }
        pinnedItems.clear()
        pinnedItems.addAll(ordered)
        pinnedItems.addAll(remaining)
    }

    val dragDropState = rememberDragDropState(
        lazyListState = listState,
        onMove = { fromIndex, toIndex ->
            val from = fromIndex - PINNED_START_INDEX
            val to = toIndex - PINNED_START_INDEX
            if (from in pinnedItems.indices && to in pinnedItems.indices && from != to) {
                pinnedItems.add(to, pinnedItems.removeAt(from))
            }
        },
        onDragFinished = {
            HyperPortalPreferences.setPinnedTargetOrder(context, pinnedItems.map { it.key })
        },
        isDragIndex = { index ->
            index >= PINNED_START_INDEX && index < PINNED_START_INDEX + pinnedItems.size
        },
    )

    LaunchedEffect(Unit) {
        allTargets = withContext(Dispatchers.IO) {
            TargetRepository.allTargetsForSettings(context)
        }
        refreshConfigAndPinned()
        loading = false
    }

    val shareTargets = remember(allTargets) {
        allTargets.filterNot { it.isBuiltIn }
    }

    val builtInMap = remember(allTargets) {
        val list = allTargets.ifEmpty { TargetRepository.allBuiltInEntries() }
        list.filter { it.isBuiltIn }.associateBy { it.key }
    }

    val appGroups = remember(shareTargets) {
        shareTargets.groupBy { it.packageName }.map { (pkg, targets) ->
            val appName = targets.firstOrNull()?.appName?.takeIf { it.isNotBlank() }
                ?: targets.firstOrNull()?.label?.takeIf { it.isNotBlank() }
                ?: pkg
            AppTargetGroup(
                packageName = pkg,
                appName = appName,
                targets = targets.sortedBy { it.label.lowercase() },
            )
        }.sortedBy { it.appName.lowercase() }
    }

    val backdrop = rememberMiuixBlurBackdrop()
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "拖拽目标",
                subtitle = "置顶优先，最多显示 ${config.targetLimit} 个",
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Back,
                            contentDescription = "返回",
                        )
                    }
                },
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
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .dragContainer(dragDropState),
            state = listState,
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
            ),
        ) {
            item(key = "title_target") {
                SmallTitle(text = "显示与推荐")
            }
            item(key = "card_limit") {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    SliderPreference(
                        title = "最多显示数量",
                        summary = "$targetLimit 个（置顶项始终显示）",
                        value = targetLimit.toFloat(),
                        valueRange = HyperPortalPreferences.MIN_TARGET_LIMIT.toFloat()..
                            HyperPortalPreferences.MAX_TARGET_LIMIT.toFloat(),
                        steps = HyperPortalPreferences.MAX_TARGET_LIMIT -
                            HyperPortalPreferences.MIN_TARGET_LIMIT - 1,
                        onValueChange = { raw ->
                            val rounded = Math.round(raw).coerceIn(
                                HyperPortalPreferences.MIN_TARGET_LIMIT,
                                HyperPortalPreferences.MAX_TARGET_LIMIT,
                            )
                            targetLimit = rounded
                            HyperPortalPreferences.setTargetLimit(context, rounded)
                            config = HyperPortalPreferences.getConfig(context)
                        },
                    )
                    SwitchPreference(
                        title = "同一应用仅推荐一项",
                        summary = "推荐时每个应用最多显示一个目标（手动置顶项不受限制）",
                        checked = config.deduplicateAppTargets,
                        onCheckedChange = { checked ->
                            HyperPortalPreferences.setDeduplicateAppTargets(context, checked)
                            config = HyperPortalPreferences.getConfig(context)
                        },
                    )
                }
            }

            item(key = "title_builtin") {
                SmallTitle(text = "内置功能")
            }
            item(key = "card_builtin") {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    BuiltInPinRow(
                        target = builtInMap["builtin:copy"],
                        title = "复制",
                        summary = "复制文本或图片，按拖拽内容自动适配",
                        key = "builtin:copy",
                        config = config,
                        onChanged = { refreshConfigAndPinned() },
                    )
                    BuiltInPinRow(
                        target = builtInMap["builtin:transfer_dock"],
                        title = "存入中转站",
                        summary = "把拖拽内容保存到中转站，可随时取回",
                        key = "builtin:transfer_dock",
                        config = config,
                        onChanged = { refreshConfigAndPinned() },
                    )
                    BuiltInPinRow(
                        target = builtInMap["builtin:save"],
                        title = "保存原图",
                        summary = "图片拖拽时保存到相册",
                        key = "builtin:save",
                        config = config,
                        onChanged = { refreshConfigAndPinned() },
                    )
                    BuiltInPinRow(
                        target = builtInMap["builtin:segment"],
                        title = "文本分词",
                        summary = "文本拖拽时打开分词面板",
                        key = "builtin:segment",
                        config = config,
                        onChanged = { refreshConfigAndPinned() },
                    )
                    BuiltInPinRow(
                        target = builtInMap["builtin:share"],
                        title = "系统分享",
                        summary = "打开系统原生分享面板，支持文本与图片",
                        key = "builtin:share",
                        config = config,
                        onChanged = { refreshConfigAndPinned() },
                    )
                    BuiltInPinRow(
                        target = builtInMap["builtin:search"],
                        title = "搜索",
                        summary = "文本拖拽时使用系统搜索",
                        key = "builtin:search",
                        config = config,
                        onChanged = { refreshConfigAndPinned() },
                    )
                    BuiltInPinRow(
                        target = builtInMap["builtin:qr"],
                        title = "识别二维码",
                        summary = "拖拽图片包含二维码时动态显示扫码入口",
                        key = "builtin:qr",
                        config = config,
                        onChanged = { refreshConfigAndPinned() },
                    )
                }
            }

            item(key = "title_pinned") {
                SmallTitle(text = "置顶项 · 长按拖动排序")
            }
            if (pinnedItems.isEmpty()) {
                item(key = "pinned_empty") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "暂无置顶项，点击下方应用的图钉即可置顶",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = pinnedItems,
                    key = { _, target -> "pinned:${target.key}" },
                ) { index, target ->
                    DraggableItem(
                        dragDropState = dragDropState,
                        index = index + PINNED_START_INDEX,
                    ) { isDragging ->
                        PinnedTargetRow(
                            target = target,
                            isDragging = isDragging,
                            onUnpin = {
                                HyperPortalPreferences.setTargetPinned(context, target.key, false)
                                refreshConfigAndPinned()
                            },
                        )
                    }
                }
            }

            item(key = "title_share") {
                SmallTitle(text = "应用入口与推荐（开启推荐 / 点击图钉置顶）")
            }
            if (loading) {
                item(key = "share_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "正在查找分享入口…",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            } else if (appGroups.isEmpty()) {
                item(key = "share_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "没有找到可分享的应用",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            } else {
                items(appGroups, key = { "app:${it.packageName}" }) { group ->
                    if (group.targets.size == 1) {
                        SingleTargetAppRow(
                            target = group.targets.first(),
                            appName = group.appName,
                            config = config,
                            onPinToggle = { pinned ->
                                HyperPortalPreferences.setTargetPinned(context, group.targets.first().key, pinned)
                                refreshConfigAndPinned()
                            },
                            onVisibleToggle = { visible ->
                                HyperPortalPreferences.setTargetVisible(context, group.targets.first().key, visible)
                                refreshConfigAndPinned()
                            },
                        )
                    } else {
                        val isExpanded = expandedAppKeys.contains(group.packageName)
                        MultiTargetAppCard(
                            group = group,
                            config = config,
                            expanded = isExpanded,
                            onToggleExpand = {
                                expandedAppKeys = if (isExpanded) {
                                    expandedAppKeys - group.packageName
                                } else {
                                    expandedAppKeys + group.packageName
                                }
                            },
                            onPinToggle = { target, pinned ->
                                HyperPortalPreferences.setTargetPinned(context, target.key, pinned)
                                refreshConfigAndPinned()
                            },
                            onVisibleToggle = { target, visible ->
                                HyperPortalPreferences.setTargetVisible(context, target.key, visible)
                                refreshConfigAndPinned()
                            },
                        )
                    }
                }
            }

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

@Composable
private fun PinnedTargetRow(
    target: ShareTargetEntry,
    isDragging: Boolean,
    onUnpin: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        cornerRadius = if (isDragging) 16.dp else 12.dp,
        holdDownState = isDragging,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TargetIcon(target = target)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = target.label,
                    style = MiuixTheme.textStyles.headline1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        target.isBuiltIn -> "系统内置功能"
                        target.appName.isNotBlank() && target.appName != target.label ->
                            target.appName + " · " + (if (target.activitySimpleName().isNotEmpty()) {
                                target.activitySimpleName()
                            } else target.packageName)
                        target.activitySimpleName().isNotEmpty() -> target.activitySimpleName()
                        else -> target.packageName
                    },
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onUnpin,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = "取消置顶",
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "≡",
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
    }
}

@Composable
private fun BuiltInPinRow(
    target: ShareTargetEntry? = null,
    title: String,
    summary: String,
    key: String,
    config: HyperPortalPreferences.Config,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    val isEnabled = if (key == "builtin:qr") {
        config.enableQrScan && !config.hiddenTargetKeys.contains(key)
    } else {
        config.pinnedTargetKeys.contains(key) && !config.hiddenTargetKeys.contains(key)
    }
    SwitchPreference(
        title = title,
        summary = summary,
        checked = isEnabled,
        onCheckedChange = { checked ->
            if (key == "builtin:qr") {
                HyperPortalPreferences.setEnableQrScan(context, checked)
                HyperPortalPreferences.setTargetVisible(context, key, checked)
                if (checked) {
                    HyperPortalPreferences.setTargetPinned(context, key, true)
                }
            } else {
                HyperPortalPreferences.setTargetPinned(context, key, checked)
                HyperPortalPreferences.setTargetVisible(context, key, checked)
            }
            onChanged()
        },
        startAction = target?.let {
            {
                TargetIcon(target = it, size = 32)
            }
        },
    )
}

@Composable
private fun SingleTargetAppRow(
    target: ShareTargetEntry,
    appName: String,
    config: HyperPortalPreferences.Config,
    onPinToggle: (Boolean) -> Unit,
    onVisibleToggle: (Boolean) -> Unit,
) {
    val isPinned = config.pinnedTargetKeys.contains(target.key)
    val isVisible = !config.hiddenTargetKeys.contains(target.key)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TargetIcon(target = target)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (target.label.isNotBlank()) target.label else appName,
                    style = MiuixTheme.textStyles.headline1,
                    color = if (isVisible) MiuixTheme.colorScheme.onSurface
                        else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        !isVisible -> "已停用推荐 · 彻底隐藏"
                        isPinned -> "$appName · 已置顶"
                        else -> appName
                    },
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { onPinToggle(!isPinned) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (isPinned) "取消置顶" else "置顶",
                    tint = if (isPinned) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Switch(
                checked = isVisible,
                onCheckedChange = onVisibleToggle,
            )
        }
    }
}

@Composable
private fun MultiTargetAppCard(
    group: AppTargetGroup,
    config: HyperPortalPreferences.Config,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onPinToggle: (ShareTargetEntry, Boolean) -> Unit,
    onVisibleToggle: (ShareTargetEntry, Boolean) -> Unit,
) {
    val totalCount = group.targets.size
    val enabledCount = group.targets.count { !config.hiddenTargetKeys.contains(it.key) }
    val pinnedCount = group.targets.count { config.pinnedTargetKeys.contains(it.key) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TargetIcon(target = group.targets.first())
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.appName,
                        style = MiuixTheme.textStyles.headline1,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "已启用 $enabledCount/$totalCount 个${if (pinnedCount > 0) " · 已置顶 $pinnedCount 个" else ""}",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "▼",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier
                        .rotate(rotationState)
                        .padding(horizontal = 8.dp),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    group.targets.forEach { target ->
                        val isPinned = config.pinnedTargetKeys.contains(target.key)
                        val isVisible = !config.hiddenTargetKeys.contains(target.key)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TargetIcon(target = target, size = 32)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = target.label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isVisible) MiuixTheme.colorScheme.onSurface
                                        else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        !isVisible -> "已停用推荐"
                                        isPinned -> "已置顶 · " + target.activitySimpleName()
                                        target.activitySimpleName().isNotEmpty() -> target.activitySimpleName()
                                        else -> target.packageName
                                    },
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(
                                onClick = { onPinToggle(target, !isPinned) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (isPinned) "取消置顶" else "置顶",
                                    tint = if (isPinned) MiuixTheme.colorScheme.primary
                                        else MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = isVisible,
                                onCheckedChange = { onVisibleToggle(target, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TargetIcon(target: ShareTargetEntry, size: Int = 40) {
    val bitmap = remember(target.key) {
        target.displayIcon()?.let { drawable: Drawable ->
            runCatching { drawable.toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(size.dp),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .background(MiuixTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = target.label.firstOrNull()?.toString()
                    ?: target.appName.firstOrNull()?.toString()
                    ?: "?",
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
        }
    }
}
