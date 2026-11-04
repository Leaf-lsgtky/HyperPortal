// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * Miuix action sheets for the transfer dock.
 *
 * The transfer dock panel itself is a classic View hierarchy hosted in an
 * overlay window.  Its action menus, however, should follow HyperOS: this
 * host puts a full-screen transparent platform window on top and renders a
 * real [OverlayDialog] inside a Miuix [Scaffold].  A Compose owner is attached
 * manually because the host is not an Activity.
 */
class TransferDockDialogHost(private val context: Context) {

    private var dialog: Dialog? = null

    fun showItemOptions(
        item: TransferDockItem,
        onCopy: Runnable,
        onShare: Runnable,
        onDelete: Runnable,
    ) {
        val title = item.title.ifBlank { "中转站" }
        val summary = item.detail.takeIf { it.isNotBlank() }
        show(
            title = title,
            summary = summary,
            actions = listOf(
                DialogAction("复制", MiuixIcons.Copy) { onCopy.run() },
                DialogAction("分享", MiuixIcons.Share) { onShare.run() },
                DialogAction("删除", MiuixIcons.Delete) { onDelete.run() },
            ),
        )
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    private fun show(
        title: String,
        summary: String?,
        actions: List<DialogAction>,
    ) {
        dismiss()

        val composeOwner = DialogComposeOwner()
        composeOwner.start()

        val hostDialog = Dialog(
            context,
            android.R.style.Theme_DeviceDefault_Dialog_NoActionBar,
        )
        hostDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        hostDialog.setCanceledOnTouchOutside(true)

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(composeOwner)
            setViewTreeSavedStateRegistryOwner(composeOwner)
            setViewTreeViewModelStoreOwner(composeOwner)
            setContent {
                val controller = remember { ThemeController(ColorSchemeMode.System) }
                MiuixTheme(controller = controller) {
                    TransferDockActionDialog(
                        title = title,
                        summary = summary,
                        actions = actions,
                        onDismissFinished = { hostDialog.dismiss() },
                    )
                }
            }
        }
        hostDialog.setContentView(
            composeView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        hostDialog.setOnDismissListener {
            composeOwner.destroy()
            if (dialog === hostDialog) {
                dialog = null
            }
        }

        hostDialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.addFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            )
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
            window.setGravity(Gravity.FILL)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val attributes = window.attributes
                attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                window.attributes = attributes
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            }
            window.setType(resolveWindowType())
        }

        dialog = hostDialog
        try {
            hostDialog.show()
            hostDialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
        } catch (t: Throwable) {
            runCatching { hostDialog.dismiss() }
            composeOwner.destroy()
            dialog = null
            throw t
        }
    }

    private fun resolveWindowType(): Int {
        if ("com.android.systemui" == context.packageName) {
            return 2024 // TYPE_NAVIGATION_BAR_PANEL
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

}

private data class DialogAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun TransferDockActionDialog(
    title: String,
    summary: String?,
    actions: List<DialogAction>,
    onDismissFinished: () -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    // Miuix dialogs install a predictive-back handler.  The dialog host is a
    // plain overlay window (not an Activity), so there is no AndroidView
    // dispatcher owner to inherit; provide one for this composition.
    val navigationOwner = remember {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = NavigationEventDispatcher()
        }
    }
    DisposableEffect(navigationOwner) {
        onDispose { navigationOwner.navigationEventDispatcher.dispose() }
    }

    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides navigationOwner) {
        Scaffold(containerColor = Color.Transparent) {
            OverlayDialog(
            show = visible,
            title = title,
            summary = summary,
            onDismissRequest = { visible = false },
            onDismissFinished = {
                val action = pendingAction
                pendingAction = null
                action?.invoke()
                onDismissFinished()
            },
            // The dialog is hosted in a full-screen system overlay.  There is
            // no activity window providing navigation-bar insets here, so use
            // the same visual bottom gap as the KernelSU reference explicitly.
            outsideMargin = DpSize(12.dp, 38.dp),
            insideMargin = DpSize(0.dp, 24.dp),
            defaultWindowInsetsPadding = false,
        ) {
            Column(Modifier.fillMaxWidth()) {
                actions.forEach { action ->
                    ArrowPreference(
                        title = action.title,
                        onClick = {
                            pendingAction = action.onClick
                            visible = false
                        },
                        startAction = {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        },
                        insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
                TextButton(
                    text = "取消",
                    onClick = { visible = false },
                    colors = ButtonDefaults.textButtonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp)
                        .padding(horizontal = 24.dp),
                )
            }
        }
    }
    }
}

private class DialogComposeOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    fun start() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) return
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}
