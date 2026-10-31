// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.github.leaf.hyperportal.data.HyperPortalPreferences
import com.github.leaf.hyperportal.util.miuixBlurEffect
import com.github.leaf.hyperportal.util.rememberMiuixBlurBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

class BlacklistActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiuixTheme {
                BlacklistScreen(onClose = { finish() })
            }
        }
    }
}

private data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

@Composable
private fun BlacklistScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var blacklist by remember {
        mutableStateOf(HyperPortalPreferences.getConfig(context).blacklist)
    }
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveList = pm.queryIntentActivities(intent, 0)
            val seen = mutableSetOf<String>()
            val result = mutableListOf<AppItem>()

            for (ri in resolveList) {
                val pkg = ri.activityInfo.packageName
                if (seen.add(pkg) && pkg != context.packageName) {
                    val label = ri.loadLabel(pm).toString()
                    val icon = runCatching { ri.loadIcon(pm) }.getOrNull()
                    result.add(AppItem(pkg, label, icon))
                }
            }

            for (pkg in blacklist) {
                if (seen.add(pkg)) {
                    val label = runCatching {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        appInfo.loadLabel(pm).toString()
                    }.getOrDefault(pkg)
                    val icon = runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
                    result.add(AppItem(pkg, label, icon))
                }
            }

            apps = result
            isLoading = false
        }
    }

    val filteredApps = remember(apps, searchQuery, blacklist) {
        val query = searchQuery.trim().lowercase()
        apps.filter {
            query.isEmpty() || it.label.lowercase().contains(query) || it.packageName.lowercase().contains(query)
        }.sortedWith(
            compareByDescending<AppItem> { blacklist.contains(it.packageName) }
                .thenBy { it.label.lowercase() }
        )
    }

    val backdrop = rememberMiuixBlurBackdrop()
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "手势黑名单",
                subtitle = if (blacklist.isEmpty()) "未排除任何应用" else "已在 ${blacklist.size} 个应用中禁用",
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
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = lazyListState,
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
            ),
        ) {
            // Search field
            item(key = "search_field") {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "搜索应用或包名…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                )
            }

            if (isLoading) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        InfiniteProgressIndicator()
                    }
                }
            } else if (filteredApps.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "未找到匹配应用" else "暂无应用",
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            } else {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isChecked = blacklist.contains(app.packageName)
                    val iconBitmap = remember(app.packageName) {
                        runCatching {
                            app.icon?.toBitmap(96, 96)?.asImageBitmap()
                        }.getOrNull()
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updated = blacklist.toMutableSet()
                                    if (isChecked) {
                                        updated.remove(app.packageName)
                                    } else {
                                        updated.add(app.packageName)
                                    }
                                    blacklist = updated
                                    HyperPortalPreferences.setBlacklist(context, updated)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .squircleClip(8.dp),
                                )
                            } else {
                                Spacer(modifier = Modifier.size(40.dp))
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MiuixTheme.textStyles.headline1,
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = app.packageName,
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }

                            Switch(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val updated = blacklist.toMutableSet()
                                    if (checked) {
                                        updated.add(app.packageName)
                                    } else {
                                        updated.remove(app.packageName)
                                    }
                                    blacklist = updated
                                    HyperPortalPreferences.setBlacklist(context, updated)
                                },
                            )
                        }
                    }
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
