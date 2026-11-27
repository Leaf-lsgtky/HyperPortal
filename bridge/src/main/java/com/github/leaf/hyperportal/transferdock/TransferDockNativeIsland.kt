// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import io.github.libxposed.api.XposedInterface
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.Executor

/** Uses a dedicated notification slot so native island templates and HyperIsland can coexist. */
internal class TransferDockNativeIsland(
    private val context: Context,
    private val worker: Executor,
) {
    private var published: Pair<String, String>? = null

    fun show(left: String?, right: String) {
        val next = left?.let { it to right }
        if (next == published) return
        val first = published == null
        published = next
        worker.execute {
            try {
                val manager = context.getSystemService(NotificationManager::class.java)
                if (next == null) {
                    manager.cancel(CHANNEL, NOTIFICATION_ID)
                    return@execute
                }
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL, "HyperPortal 中转站", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        setSound(null, null)
                        enableVibration(false)
                        setShowBadge(false)
                        lockscreenVisibility = Notification.VISIBILITY_SECRET
                    },
                )
                val picture = JSONObject().put("type", 1).put("pic", PICTURE)
                val leftModule = JSONObject().put("type", 1).apply {
                    if (next.first.isEmpty()) put("picInfo", picture)
                    else put("textInfo", JSONObject().put("title", next.first))
                }
                val rightModule = JSONObject().put("type", 2)
                    .put("textInfo", JSONObject().put("title", next.second))
                val island = JSONObject()
                    .put("islandProperty", 0)
                    .put("islandPriority", 0)
                    .put("islandTimeout", Int.MAX_VALUE)
                    .put("bigIslandArea", JSONObject()
                        .put("imageTextInfoLeft", leftModule)
                        .put("imageTextInfoRight", rightModule))
                    .put("smallIslandArea", JSONObject().put("picInfo", picture))
                val focus = JSONObject()
                    .put("business", CHANNEL)
                    .put("isShowNotification", false)
                    .put("islandFirstFloat", first)
                    .put("enableFloat", false)
                    .put("updatable", true)
                    .put("param_island", island)
                val notification = Notification.Builder(context, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_menu_save)
                    .setContentTitle("中转站")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(Notification.VISIBILITY_SECRET)
                    .build()
                notification.extras.putString("miui.focus.param",
                    JSONObject().put("param_v2", focus).toString())
                notification.extras.putBundle("miui.focus.pics", Bundle().apply {
                    putParcelable(PICTURE, Icon.createWithResource("android", android.R.drawable.ic_menu_save))
                })
                manager.notify(CHANNEL, NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.w(TAG, "Could not update the native transfer dock island", e)
            }
        }
    }

    fun bounds(): Rect? = activeBounds?.takeIf { activeView?.get()?.isAttachedToWindow == true }?.let(::Rect)

    fun wasRendered(): Boolean = rendered

    companion object {
        private const val TAG = "TransferDockNativeIsland"
        private const val CHANNEL = "hyperportal_transfer_dock_island"
        private const val NOTIFICATION_ID = 0x48505444
        private const val PICTURE = "hyperportal_dock"
        private const val CONTENT_VIEW = "miui.systemui.dynamicisland.window.content.DynamicIslandContentView"
        private val installed = WeakHashMap<Class<*>, Unit>()
        private var activeView: WeakReference<View>? = null
        private var activeBounds: Rect? = null
        private var rendered = false

        @JvmStatic
        fun install(xposed: XposedInterface, loader: ClassLoader, onBoundsChanged: Runnable) {
            bind(xposed, loader, onBoundsChanged)
            try {
                val factory = Class.forName(
                    "com.android.systemui.shared.plugins.PluginInstance\$PluginFactory", false, loader,
                )
                for (method in factory.declaredMethods.filter { it.name == "createPluginContext" }) {
                    xposed.hook(method).setId("hyperportal-dock-plugin-context").intercept { chain ->
                        val result = chain.proceed()
                        val plugin = result as? Context
                        if (plugin?.packageName == "miui.systemui.plugin") {
                            bind(xposed, plugin.classLoader, onBoundsChanged)
                        }
                        result
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Plug-in context observer unavailable", e)
            }
        }

        private fun bind(xposed: XposedInterface, loader: ClassLoader, onBoundsChanged: Runnable) {
            try {
                val type = Class.forName(CONTENT_VIEW, false, loader)
                val currentData = type.getMethod("getCurrentIslandData")
                val getBigIsland = type.getMethod("getBigIslandView")
                val getSmallIsland = runCatching { type.getMethod("getSmallIslandView") }.getOrNull()
                val getFakeBigIsland = runCatching { type.getMethod("getFakeBigIsland") }.getOrNull()
                val getExtras = currentData.returnType.getMethod("getExtras")
                val updates = type.declaredMethods.filter {
                    (it.name == "updateBigIslandView" || it.name == "updateSmallIslandView") &&
                        it.parameterCount == 3
                }
                synchronized(installed) {
                    if (installed.containsKey(type)) return
                    val observers = WeakHashMap<View, ViewTreeObserver.OnPreDrawListener>()
                    val visibilityChanged = type.getDeclaredMethod("onVisibilityAggregated", Boolean::class.javaPrimitiveType)
                    xposed.hook(visibilityChanged).setId("hyperportal-dock-native-visibility").intercept { chain ->
                        val result = chain.proceed()
                        val view = chain.thisObject as? View
                        if (view != null && chain.args[0] == false) updateBounds(view, null, onBoundsChanged)
                        result
                    }
                    for (update in updates) xposed.hook(update).setId("hyperportal-dock-native-${update.name}").intercept { chain ->
                        val result = chain.proceed()
                        try {
                            val content = chain.thisObject as? View
                            val data = chain.args[0]
                            if (content != null && data != null && isDock(getExtras.invoke(data))) {
                                content.post {
                                    if (!content.isAttachedToWindow || observers.containsKey(content)) return@post
                                    val reference = WeakReference(content)
                                    val observer = object : ViewTreeObserver.OnPreDrawListener {
                                        override fun onPreDraw(): Boolean {
                                            val view = reference.get() ?: return true
                                            try {
                                                val data = currentData.invoke(view)
                                                if (data == null || !isDock(getExtras.invoke(data))) {
                                                    view.viewTreeObserver.removeOnPreDrawListener(this)
                                                    observers.remove(view)
                                                    updateBounds(view, null, onBoundsChanged)
                                                } else {
                                                    val stable = getBigIsland.invoke(view) as? View
                                                    val fake = getFakeBigIsland?.invoke(view) as? View
                                                    val small = getSmallIsland?.invoke(view) as? View
                                                    val bounds = visibleRect(stable) ?: visibleRect(fake) ?: visibleRect(small)
                                                    updateBounds(view, bounds, onBoundsChanged)
                                                }
                                            } catch (e: Exception) {
                                                Log.d(TAG, "Native transfer dock bounds unavailable", e)
                                            }
                                            return true
                                        }
                                    }
                                    observers[content] = observer
                                    content.viewTreeObserver.addOnPreDrawListener(observer)
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Could not track the native transfer dock island", e)
                        }
                        result
                    }
                    installed[type] = Unit
                }
            } catch (_: ClassNotFoundException) {
                // The plug-in normally loads after SystemUI.
            } catch (e: LinkageError) {
                Log.d(TAG, "Native transfer dock classes are not ready", e)
            } catch (e: Exception) {
                Log.d(TAG, "Native transfer dock view unavailable", e)
            }
        }

        private fun visibleRect(view: View?): Rect? {
            if (view?.isShown != true || view.alpha <= 0f) return null
            val rect = Rect()
            return if (view.getGlobalVisibleRect(rect) && !rect.isEmpty) rect else null
        }

        private fun updateBounds(view: View, bounds: Rect?, changed: Runnable) {
            if (bounds == null && activeView?.get() !== view) return
            if (bounds == activeBounds && activeView?.get() === view) return
            if (bounds != null && !rendered) {
                rendered = true
                Log.i(TAG, "Native transfer dock island visible")
            }
            activeView = if (bounds == null) null else WeakReference(view)
            activeBounds = bounds
            changed.run()
        }

        private fun isDock(extras: Any?): Boolean {
            val notification = (extras as? Bundle)?.getParcelable("miui.sbn", StatusBarNotification::class.java)
            return notification?.packageName == "com.android.systemui" &&
                notification.tag == CHANNEL && notification.id == NOTIFICATION_ID
        }
    }
}
