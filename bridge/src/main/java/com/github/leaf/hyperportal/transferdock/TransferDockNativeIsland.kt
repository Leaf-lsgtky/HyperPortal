// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.RemoteViews
import com.github.leaf.hyperportal.R
import com.github.leaf.hyperportal.target.BuiltInIconDrawable
import io.github.libxposed.api.XposedInterface
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.WeakHashMap
import java.util.concurrent.Executor
import java.util.function.Supplier

/** The plug-in owns the island window, the focus views and their transitions. */
internal class TransferDockNativeIsland(private val context: Context, private val worker: Executor) {
    private var published: String? = null
    private val dockIcon by lazy {
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        BuiltInIconDrawable.create(BuiltInIconDrawable.TYPE_TRANSFER_DOCK).apply {
            setBounds(0, 0, bitmap.width, bitmap.height)
            draw(Canvas(bitmap))
        }
        Icon.createWithBitmap(bitmap)
    }

    fun show(right: String?) {
        if (right == published) return
        val updating = published != null
        published = right
        worker.execute {
            try {
                val manager = context.getSystemService(NotificationManager::class.java)
                if (right == null) {
                    manager.cancel(CHANNEL, NOTIFICATION_ID)
                    return@execute
                }
                if (!updating) {
                    manager.createNotificationChannel(
                        NotificationChannel(CHANNEL, TITLE, NotificationManager.IMPORTANCE_DEFAULT).apply {
                            setSound(null, null)
                            enableVibration(false)
                            setShowBadge(false)
                            lockscreenVisibility = Notification.VISIBILITY_SECRET
                        },
                    )
                }
                val picture = JSONObject().put("type", 1).put("pic", PICTURE)
                val island = JSONObject()
                    .put("business", CHANNEL)
                    // Property 1 uses the recoverable BIG/SMALL states on HyperOS.
                    .put("islandProperty", 1)
                    .put("islandPriority", 1)
                    .put("islandTimeout", Int.MAX_VALUE)
                    .put("bigIslandArea", JSONObject()
                        .put("imageTextInfoLeft", JSONObject().put("type", 1)
                            .put("picInfo", picture)
                            .put("textInfo", JSONObject().put("title", TITLE)))
                        .put("imageTextInfoRight", JSONObject().put("type", 2)
                            .put("textInfo", JSONObject().put("title", right))))
                    .put("smallIslandArea", JSONObject().put("picInfo", picture))
                val focus = JSONObject()
                    .put("isShowNotification", false)
                    .put("islandFirstFloat", false)
                    .put("enableFloat", false)
                    .put("updatable", true)
                    .put("param_island", island)
                val panel = RemoteViews(PORTAL_PACKAGE, R.layout.hyperportal_transfer_dock_focus)
                val notification = Notification.Builder(context, CHANNEL)
                    .setSmallIcon(dockIcon)
                    .setContentTitle(TITLE)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(Notification.VISIBILITY_SECRET)
                    .addExtras(Bundle().apply {
                        putParcelable("miui.focus.rv", panel)
                        putParcelable("miui.focus.rv.island.expand", panel)
                        putString("miui.focus.param.custom", focus.toString())
                        putBoolean("miui.island.firstFloat", false)
                        putBoolean("miui.island.updateNoFloat", true)
                        putBoolean("miui.enableFloat", false)
                        putBoolean("show_notification", false)
                        putBundle("miui.focus.pics", Bundle().apply { putParcelable(PICTURE, dockIcon) })
                    })
                    .build()
                manager.notify(CHANNEL, NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.w(TAG, "Could not update the native transfer dock island", e)
            }
        }
    }

    companion object {
        private const val TAG = "TransferDockNativeIsland"
        private const val PORTAL_PACKAGE = "com.github.leaf.hyperportal"
        private const val CHANNEL = "hyperportal_transfer_dock_island"
        private const val NOTIFICATION_ID = 0x48505444
        private const val TITLE = "中转站"
        private const val PICTURE = "hyperportal_dock"
        private const val WINDOW_VIEW = "miui.systemui.dynamicisland.window.DynamicIslandWindowView"
        private const val CONTENT_PACKAGE = "miui.systemui.dynamicisland.window.content"
        private val installed = WeakHashMap<Class<*>, Unit>()
        private var summary: WeakReference<View>? = null
        private var hitRect: Rect? = null
        private var isCurrentSummary: ((View) -> Boolean)? = null
        private var openSummary: Runnable? = null

        @JvmStatic
        fun install(xposed: XposedInterface, loader: ClassLoader, dock: Supplier<TransferDockIslandController?>) {
            try {
                // The window inherits this method. The stock result is preserved for all
                // other windows and drags, including drops over a different native island.
                val dispatch = ViewGroup::class.java.getDeclaredMethod("dispatchDragEvent", DragEvent::class.java)
                xposed.hook(dispatch).setId("hyperportal-dock-native-drag").intercept { chain ->
                    val window = chain.thisObject as? View
                    if (window?.javaClass?.name != WINDOW_VIEW) return@intercept chain.proceed()
                    val event = chain.args[0] as? DragEvent ?: return@intercept chain.proceed()
                    val controller = dock.get() ?: return@intercept chain.proceed()
                    if (event.action == DragEvent.ACTION_DRAG_STARTED) {
                        val nativeResult = chain.proceed()
                        return@intercept guard { controller.onNativeDrag(window, event) } == true || nativeResult == true
                    }
                    if (guard { controller.onNativeDrag(window, event) } == true) true else chain.proceed()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Native island drag dispatch unavailable", e)
            }
            bind(xposed, loader, dock)
            try {
                val factory = Class.forName(
                    "com.android.systemui.shared.plugins.PluginInstance\$PluginFactory", false, loader,
                )
                for (method in factory.declaredMethods.filter { it.name == "createPluginContext" }) {
                    xposed.hook(method).setId("hyperportal-dock-plugin-context").intercept { chain ->
                        val result = chain.proceed()
                        val plugin = result as? Context
                        if (plugin?.packageName == "miui.systemui.plugin") bind(xposed, plugin.classLoader, dock)
                        result
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Plug-in context observer unavailable", e)
            }
        }

        fun hit(x: Float, y: Float): Boolean {
            if (hitRect?.contains(x.toInt(), y.toInt()) != true) return false
            val view = summary?.get() ?: return false
            return view.isAttachedToWindow && guard { isCurrentSummary?.invoke(view) } == true
        }

        fun requestOpen() {
            openSummary?.run()
        }

        private fun bind(xposed: XposedInterface, loader: ClassLoader, dock: Supplier<TransferDockIslandController?>) {
            try {
                val real = Class.forName("$CONTENT_PACKAGE.DynamicIslandContentView", false, loader)
                val fake = Class.forName("$CONTENT_PACKAGE.DynamicIslandContentFakeView", false, loader)
                val base = real.superclass ?: return
                val window = Class.forName(WINDOW_VIEW, false, loader)
                val currentData = base.getMethod("getCurrentIslandData")
                val state = base.getMethod("getState")
                val data = currentData.returnType
                val extras = data.getMethod("getExtras")
                val click = real.getMethod("onIslandClick")
                val big = base.getMethod("getBigIslandView")
                val small = base.getMethod("getSmallIslandView")
                val fakeBig = base.getMethod("getFakeBigIsland")
                val longPress = window.declaredMethods.first { it.name == "onLongPress" && it.parameterCount == 3 }
                val expanded = window.getMethod("getCurrentExpandedState")
                val dispatchTouch = window.getDeclaredMethod("dispatchTouchEvent", MotionEvent::class.java)
                val observer = SummaryObserver(currentData, extras, state, click, big, small, fakeBig, dock)

                synchronized(installed) {
                    if (installed.containsKey(real)) return
                    for (method in real.declaredMethods.filter {
                        (it.name == "updateBigIslandView" || it.name == "updateSmallIslandView") && it.parameterCount == 3
                    }) {
                        xposed.hook(method).setId("hyperportal-dock-summary-${method.name}").intercept { chain ->
                            val result = chain.proceed()
                            guard {
                                val view = chain.thisObject as? View
                                if (view != null && isDock(chain.args[0], extras)) observer.attach(view)
                            }
                            result
                        }
                    }
                    hookExpansion(xposed, real, data.getMethod("getView"), extras, dock, true)
                    hookExpansion(xposed, fake, data.getMethod("getFakeView"), extras, dock, false)
                    hookCustomFocusAuth(xposed, loader)
                    xposed.hook(dispatchTouch).setId("hyperportal-dock-panel-touch").intercept { chain ->
                        val handled = guard {
                            val event = chain.args[0] as? MotionEvent ?: return@guard false
                            val view = chain.thisObject as? View ?: return@guard false
                            val controller = dock.get() ?: return@guard false
                            val mayStart = if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                                val owner = expanded.invoke(view) ?: return@guard false
                                isDock(currentData.invoke(owner), extras)
                            } else false
                            controller.dispatchPanelTouch(view, event, mayStart)
                        }
                        if (handled == true) true else chain.proceed()
                    }
                    hookExternalTouch(xposed, loader, expanded, currentData, extras, dock)
                    xposed.hook(longPress).setId("hyperportal-dock-long-press").intercept { chain ->
                        val handled = guard {
                            val pressed = chain.args[0] as? View
                            pressed != null && real.isInstance(pressed) &&
                                isDock(currentData.invoke(pressed), extras) && isSummary(state.invoke(pressed)) &&
                                dock.get()?.dragLatest(pressed) == true
                        }
                        if (handled == true) null else chain.proceed()
                    }
                    installed[real] = Unit
                }
            } catch (_: ClassNotFoundException) {
                // The plug-in normally loads after SystemUI.
            } catch (e: LinkageError) {
                Log.d(TAG, "Native transfer dock classes are not ready", e)
            } catch (e: Exception) {
                Log.w(TAG, "Native transfer dock view unavailable", e)
            }
        }

        private fun hookExternalTouch(
            xposed: XposedInterface, loader: ClassLoader, expanded: Method,
            currentData: Method, extras: Method, dock: Supplier<TransferDockIslandController?>,
        ) {
            guard {
                val interactor = Class.forName(
                    "miui.systemui.dynamicisland.touch.domain.interactor.DynamicIslandTouchInteractor", false, loader,
                )
                val windowField = interactor.getDeclaredField("windowView").apply { isAccessible = true }
                for (name in listOf("onInterceptTouchEvent", "onTouchEvent")) {
                    val method = interactor.getDeclaredMethod(name, MotionEvent::class.java, String::class.java)
                    xposed.hook(method).setId("hyperportal-dock-list-$name").intercept { chain ->
                        val handled = guard {
                            val event = chain.args[0] as? MotionEvent ?: return@guard false
                            val window = windowField.get(chain.thisObject) as? View ?: return@guard false
                            val owner = expanded.invoke(window) ?: return@guard false
                            isDock(currentData.invoke(owner), extras) &&
                                dock.get()?.isPanelGesture(window, event) == true
                        }
                        if (handled == true) false else chain.proceed()
                    }
                }
            }
        }

        fun collapsePanel(window: View) {
            guard {
                if (window.javaClass.name != WINDOW_VIEW) return@guard
                val owner = window.javaClass.getMethod("getCurrentExpandedState").invoke(window) ?: return@guard
                val getData = owner.javaClass.getMethod("getCurrentIslandData")
                if (isDock(getData.invoke(owner), getData.returnType.getMethod("getExtras"))) {
                    window.javaClass.getMethod("collapse", String::class.java).invoke(window, "transferDockClear")
                }
            }
        }

        private fun hookCustomFocusAuth(xposed: XposedInterface, loader: ClassLoader) {
            guard {
                val controller = Class.forName("miui.systemui.notification.focus.FocusNotificationController", false, loader)
                val authorize = controller.declaredMethods.first { it.name == "fetchAuthResult" && it.parameterCount == 5 }
                val onSuccess = authorize.parameterTypes[4].getMethod("onAuthSuccess", String::class.java, String::class.java)
                xposed.hook(authorize).setId("hyperportal-dock-focus-auth").intercept { chain ->
                    val notification = chain.args[1] as? StatusBarNotification
                    val handled = guard {
                        if (!isDock(notification)) return@guard false
                        val callback = chain.args[4] ?: return@guard false
                        onSuccess.invoke(callback, notification?.key, chain.args[2])
                        true
                    }
                    if (handled == true) null else chain.proceed()
                }
            }
        }

        private fun hookExpansion(
            xposed: XposedInterface, type: Class<*>, getView: Method, extras: Method,
            dock: Supplier<TransferDockIslandController?>, interactive: Boolean,
        ) {
            val method = type.declaredMethods.first { it.name == "updateExpandedView" && it.parameterCount == 3 }
            xposed.hook(method).setId("hyperportal-dock-expand-${type.simpleName}").intercept { chain ->
                guard {
                    val data = chain.args[0]
                    if (isDock(data, extras)) {
                        val content = getView.invoke(data) as? View
                        content?.findViewById<ViewGroup>(R.id.hyperportal_transfer_dock_content)?.let {
                            dock.get()?.bindPanel(it, interactive)
                        }
                    }
                }
                chain.proceed()
            }
        }

        private class SummaryObserver(
            private val currentData: Method, private val extras: Method, private val state: Method,
            private val click: Method, private val big: Method, private val small: Method,
            private val fakeBig: Method, private val dock: Supplier<TransferDockIslandController?>,
        ) {
            private val tracked = WeakHashMap<View, Unit>()

            fun attach(content: View) {
                content.post {
                    if (!content.isAttachedToWindow || tracked.containsKey(content)) return@post
                    tracked[content] = Unit
                    val reference = WeakReference(content)
                    content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            val view = reference.get() ?: return true
                            guard {
                                if (!isDock(currentData.invoke(view), extras)) {
                                    view.viewTreeObserver.removeOnPreDrawListener(this)
                                    tracked.remove(view)
                                    update(view, null)
                                } else {
                                    val currentState = state.invoke(view)
                                    val bounds = if (isSummary(currentState)) {
                                        val smallState = currentState.javaClass.simpleName == "SmallIsland"
                                        if (smallState) visibleBounds(small.invoke(view) as? View)
                                        else visibleBounds(big.invoke(view) as? View)
                                            ?: visibleBounds(fakeBig.invoke(view) as? View)
                                    } else null
                                    update(view, bounds)
                                }
                            }
                            return true
                        }
                    })
                }
            }

            private fun update(view: View, bounds: Rect?) {
                if (bounds == null && summary?.get() !== view) return
                if (bounds == hitRect && summary?.get() === view) return
                summary = if (bounds == null) null else WeakReference(view)
                hitRect = bounds
                isCurrentSummary = if (bounds == null) null else { target ->
                    isDock(currentData.invoke(target), extras) && isSummary(state.invoke(target))
                }
                openSummary = if (bounds == null) null else Runnable {
                    guard {
                        val active = summary?.get()
                        if (active?.isShown == true && isDock(currentData.invoke(active), extras)
                            && isSummary(state.invoke(active))) {
                            click.invoke(active)
                        }
                    }
                }
                dock.get()?.onNativeBoundsChanged()
            }
        }

        private fun visibleBounds(view: View?): Rect? {
            if (view?.isShown != true || view.alpha <= 0f) return null
            val rect = Rect()
            if (!view.getGlobalVisibleRect(rect) || rect.isEmpty) return null
            val origin = IntArray(2)
            view.rootView.getLocationOnScreen(origin)
            rect.offset(origin[0], origin[1])
            return rect
        }

        private fun isSummary(state: Any?): Boolean = when (state?.javaClass?.simpleName) {
            "BigIsland", "SmallIsland" -> true
            else -> false
        }

        private fun isDock(data: Any?, extras: Method): Boolean {
            val metadata = if (data != null) extras.invoke(data) as? Bundle else null
            return isDock(metadata?.getParcelable("miui.sbn", StatusBarNotification::class.java))
        }

        private fun isDock(sbn: StatusBarNotification?): Boolean =
            sbn?.packageName == "com.android.systemui" && sbn.uid == android.os.Process.myUid() &&
                sbn.tag == CHANNEL && sbn.id == NOTIFICATION_ID

        private inline fun <T> guard(action: () -> T): T? = try {
            action()
        } catch (e: Exception) {
            Log.d(TAG, "Transfer dock native view unavailable", e)
            null
        }
    }
}
