// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock

import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.PowerManager
import android.text.TextUtils
import android.util.Log
import android.util.LruCache
import android.view.DragEvent
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.Executor
import kotlin.math.roundToInt

internal class TransferDockIslandController(
    private val context: Context,
    private val main: Handler,
    private val worker: Executor,
    private val onDrop: (ClipData) -> Boolean,
) {
    private val keyguard = context.getSystemService(KeyguardManager::class.java)
    private val power = context.getSystemService(PowerManager::class.java)
    private val nativeIsland = TransferDockNativeIsland(context, worker)
    private val previewOutline = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            val radius = minOf(dp(CARD_CORNER_DP).toFloat(), minOf(view.width, view.height) / 2f)
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
    private val outboundToken = Any()
    private val panels = WeakHashMap<ViewGroup, Panel>()
    private val previews = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private var enabled = false
    private var incoming = false
    private var hovering = false
    private var saved = false
    private var saving = false
    private var fingerX = -1f
    private var fingerY = -1f
    private var scrollOffset = 0
    private var revision = 0
    private var clearing = false
    private var touchTarget: WeakReference<ViewGroup>? = null
    private var panelDownTime = -1L
    private var outboundWindow: WeakReference<View>? = null
    private var items = emptyList<TransferDockItem>()
    private var latestImagePreview: Bitmap? = null
    private val resetSaved = Runnable {
        saved = false
        render()
    }
    private val endIncoming = Runnable { setIncoming(false) }

    private data class Panel(val interactive: Boolean, val revision: Int)

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        if (enabled) refreshItems() else {
            setIncoming(false)
            saved = false
            saving = false
            main.removeCallbacks(resetSaved)
        }
        render()
    }

    fun setIncoming(value: Boolean) {
        if (value && (!enabled || outboundWindow != null)) return
        main.removeCallbacks(endIncoming)
        if (incoming == value) return
        incoming = value
        if (!value) {
            fingerX = -1f
            fingerY = -1f
            hovering = false
        }
        render()
    }

    fun finishIncomingSoon() {
        if (!incoming) return
        main.removeCallbacks(endIncoming)
        main.postDelayed(endIncoming, 100)
    }

    fun hoverAt(x: Float, y: Float) {
        if (!incoming || x < 0f || y < 0f || (x == 0f && y == 0f)) return
        fingerX = x
        fingerY = y
        onNativeBoundsChanged()
    }

    fun onNativeBoundsChanged() {
        if (!incoming) return
        val hit = TransferDockNativeIsland.hit(fingerX, fingerY)
        if (hovering == hit) return
        hovering = hit
        render()
    }

    fun dropAt(x: Float, y: Float, clip: ClipData?): Boolean {
        if (!incoming || !canShow() || clip == null || !TransferDockNativeIsland.hit(x, y)) return false
        if (!clip.description.hasMimeType(PORTAL_MIME)) return false
        val accepted = onDrop(clip)
        if (accepted) {
            saving = true
            setIncoming(false)
        }
        return accepted
    }

    /** Observe native window drags. Only this dock's incoming payload can consume a drop. */
    fun onNativeDrag(window: View, event: DragEvent): Boolean {
        if (event.localState === outboundToken || outboundWindow?.get() === window) {
            if (event.action == DragEvent.ACTION_DRAG_ENDED) finishOutbound()
            return event.action == DragEvent.ACTION_DRAG_STARTED
        }
        if (!enabled || !canShow()) return false
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                if (event.clipDescription?.hasMimeType(PORTAL_MIME) != true) return false
                setIncoming(true)
                return true
            }
            DragEvent.ACTION_DRAG_LOCATION, DragEvent.ACTION_DROP -> {
                if (!incoming || event.clipDescription?.hasMimeType(PORTAL_MIME) != true) return false
                val origin = IntArray(2)
                window.getLocationOnScreen(origin)
                val x = origin[0] + event.x
                val y = origin[1] + event.y
                if (event.action == DragEvent.ACTION_DROP) return dropAt(x, y, event.clipData)
                hoverAt(x, y)
            }
            DragEvent.ACTION_DRAG_EXITED -> if (incoming) {
                fingerX = -1f
                fingerY = -1f
                onNativeBoundsChanged()
            }
            DragEvent.ACTION_DRAG_ENDED -> setIncoming(false)
        }
        return false
    }

    fun onScreenChanged() {
        if (!canShow()) setIncoming(false)
        render()
    }

    fun refreshItems(justSaved: Boolean = false) {
        worker.execute {
            val snapshot = TransferDockManager.getInstance(context).getItems()
            // The system may start its expansion transition on the next tap.
            // Prepare the images that can enter the initial viewport before posting.
            for (item in snapshot.take(4)) {
                if (item.type == TransferDockItem.TYPE_IMAGE && previews.get(item.id) == null) {
                    loadPreview(item)?.let { previews.put(item.id, it) }
                }
            }
            val latestPreview = snapshot.firstOrNull()?.let { previews.get(it.id) }
            main.post {
                items = snapshot
                latestImagePreview = latestPreview
                revision++
                for ((host, panel) in panels.toList()) bindPanel(host, panel.interactive)
                if (enabled && justSaved) {
                    saving = false
                    saved = true
                    main.removeCallbacks(resetSaved)
                    main.postDelayed(resetSaved, 1800)
                }
                render()
            }
        }
    }

    fun saveFailed() {
        saving = false
        render()
    }

    fun openPanel() {
        if (enabled && canShow() && items.isNotEmpty()) TransferDockNativeIsland.requestOpen()
    }

    fun dispatchPanelTouch(window: View, event: MotionEvent, mayStart: Boolean): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            touchTarget = null
            if (!mayStart || !enabled || !canShow()) return false
            val target = panelAt(window, event.x.toInt(), event.y.toInt()) ?: return false
            touchTarget = WeakReference(target)
            panelDownTime = event.downTime
        }
        val target = touchTarget ?: return false
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            touchTarget = null
        }
        target.get()?.takeIf { it.isAttachedToWindow && it.rootView === window }?.let { host ->
            val translated = MotionEvent.obtain(event)
            try {
                val location = IntArray(2)
                window.getLocationOnScreen(location)
                val windowX = location[0]
                val windowY = location[1]
                host.getLocationOnScreen(location)
                translated.offsetLocation((windowX - location[0]).toFloat(), (windowY - location[1]).toFloat())
                host.dispatchTouchEvent(translated)
            } finally {
                translated.recycle()
            }
        }
        return true
    }

    fun isPanelGesture(window: View, event: MotionEvent): Boolean {
        if (!enabled) return false
        if (panelDownTime == event.downTime) return true
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return false
        val location = IntArray(2)
        window.getLocationOnScreen(location)
        if (panelAt(window, event.rawX.toInt() - location[0], event.rawY.toInt() - location[1]) == null) return false
        panelDownTime = event.downTime
        return true
    }

    private fun panelAt(window: View, x: Int, y: Int): ViewGroup? {
        val bounds = Rect()
        return panels.entries.firstOrNull { (host, panel) ->
            panel.interactive && host.rootView === window && host.isShown &&
                host.getGlobalVisibleRect(bounds) && bounds.contains(x, y)
        }?.key
    }

    fun bindPanel(host: ViewGroup, interactive: Boolean) {
        if (panels[host] == Panel(interactive, revision)) return
        panels[host] = Panel(interactive, revision)
        host.removeAllViews()
        if (items.isEmpty()) {
            host.addView(label(host.context, "暂无内容", 14f).apply { gravity = Gravity.CENTER })
            return
        }
        val uiContext = host.context
        val content = LinearLayout(uiContext).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val clear = ImageView(uiContext).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "清空中转站"
            background = rounded(0x247f8796, CARD_CORNER_DP)
            if (interactive) setOnClickListener { clearDock(host.rootView) }
        }
        content.addView(clear, LinearLayout.LayoutParams(dp(42), dp(48)).apply { marginEnd = dp(9) })
        val scroll = HorizontalScrollView(uiContext).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val row = LinearLayout(uiContext).apply { gravity = Gravity.CENTER_VERTICAL }
        scroll.addView(row, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        content.addView(scroll, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        host.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        if (interactive) scroll.setOnScrollChangeListener { _, x, _, _, _ -> scrollOffset = x }
        val previewsToLoad = mutableListOf<Pair<TransferDockItem, WeakReference<ImageView>>>()
        for (item in items) {
            if (item.type == TransferDockItem.TYPE_IMAGE) {
                val preview = ImageView(uiContext).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    outlineProvider = previewOutline
                    clipToOutline = true
                    contentDescription = item.title
                }
                bindItemActions(preview, item, interactive)
                row.addView(preview, LinearLayout.LayoutParams(dp(76), dp(76)).apply { marginEnd = dp(8) })
                val bitmap = previews.get(item.id)
                if (bitmap != null) showPreview(preview, bitmap, previewWidth(scroll))
                else previewsToLoad.add(item to WeakReference(preview))
                continue
            }
            val card = LinearLayout(uiContext).apply {
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = rounded(0x247f8796, CARD_CORNER_DP)
            }
            bindItemActions(card, item, interactive)
            when (item.type) {
                TransferDockItem.TYPE_TEXT -> {
                    card.addView(label(uiContext, item.text ?: item.title, 14f).apply { maxLines = 4 })
                }
                else -> {
                    card.addView(ImageView(uiContext).apply {
                        setImageResource(android.R.drawable.ic_menu_agenda)
                        contentDescription = null
                    }, LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(7) })
                    card.addView(label(uiContext, item.title, 13f).apply { maxLines = 2 })
                }
            }
            row.addView(card, LinearLayout.LayoutParams(dp(136), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginEnd = dp(8)
            })
        }
        scroll.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left == oldRight - oldLeft) return@addOnLayoutChangeListener
            val maxWidth = previewWidth(scroll)
            for (index in 0 until row.childCount) {
                val image = row.getChildAt(index) as? ImageView ?: continue
                val bitmap = (image.drawable as? BitmapDrawable)?.bitmap ?: continue
                showPreview(image, bitmap, maxWidth)
            }
        }
        scroll.post { scroll.scrollTo(scrollOffset, 0) }
        if (previewsToLoad.isEmpty()) return
        worker.execute {
            for ((item, view) in previewsToLoad) {
                val bitmap = previews.get(item.id) ?: loadPreview(item) ?: continue
                previews.put(item.id, bitmap)
                main.post {
                    view.get()?.let { showPreview(it, bitmap, previewWidth(scroll)) }
                }
            }
        }
    }

    private fun previewWidth(scroll: View): Int {
        val available = if (scroll.width > 0) scroll.width
            else (scroll.resources.displayMetrics.widthPixels - dp(104)).coerceAtLeast(dp(48))
        return minOf(dp(238), available)
    }

    private fun showPreview(view: ImageView, bitmap: Bitmap, maxWidth: Int) {
        val size = previewSize(bitmap, maxWidth, dp(76))
        val layout = view.layoutParams
        if (layout.width != size.x || layout.height != size.y) {
            layout.width = size.x
            layout.height = size.y
            view.layoutParams = layout
        }
        if ((view.drawable as? BitmapDrawable)?.bitmap !== bitmap) view.setImageBitmap(bitmap)
    }

    private fun previewSize(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Point {
        val scale = minOf(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        return Point(
            (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1),
        )
    }

    private fun bindItemActions(view: View, item: TransferDockItem, interactive: Boolean) {
        if (!interactive) return
        view.setOnClickListener { copy(item) }
        view.setOnLongClickListener { startDrag(it, item, true) }
    }

    private fun clearDock(window: View) {
        if (!enabled || !canShow() || clearing || items.isEmpty()) return
        clearing = true
        val owner = WeakReference(window)
        worker.execute {
            val cleared = TransferDockManager.getInstance(context).clearAll()
            main.post {
                clearing = false
                if (!cleared) {
                    Toast.makeText(context, "清空失败", Toast.LENGTH_SHORT).show()
                    return@post
                }
                items = emptyList()
                latestImagePreview = null
                previews.evictAll()
                scrollOffset = 0
                saved = false
                saving = false
                main.removeCallbacks(resetSaved)
                revision++
                owner.get()?.let(TransferDockNativeIsland::collapsePanel)
                render()
            }
        }
    }

    private fun loadPreview(item: TransferDockItem): Bitmap? {
        return item.filePath?.let(::decodePreview) ?: item.thumbnailPath?.let(::decodePreview)
    }

    private fun decodePreview(path: String): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        options.inSampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 320) {
            options.inSampleSize *= 2
        }
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    fun dragLatest(view: View): Boolean =
        items.firstOrNull()?.let { startDrag(view, it, false) } == true

    private fun startDrag(view: View, item: TransferDockItem, useCard: Boolean): Boolean {
        if (!enabled || incoming || outboundWindow != null || !canShow() || !view.isAttachedToWindow) return false
        val clip = clipFor(item, forDrag = true) ?: return false
        val shadow = if (useCard) View.DragShadowBuilder(view) else summaryDragShadow(view, item)
        outboundWindow = WeakReference(view.rootView)
        TransferDockDragGuard.setDragging(true)
        val started = try {
            view.startDragAndDrop(clip, shadow, outboundToken,
                View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not drag transfer dock item", e)
            false
        }
        if (!started) {
            finishOutbound()
            Log.w(TAG, "The system rejected the transfer dock drag")
        }
        return started
    }

    private fun summaryDragShadow(view: View, item: TransferDockItem): View.DragShadowBuilder {
        if (item.type == TransferDockItem.TYPE_IMAGE) latestImagePreview?.let { return imageDragShadow(it) }
        val label = label(view.context, item.title, 14f).apply {
            gravity = Gravity.CENTER
            setPadding(dp(9), 0, dp(9), 0)
            background = rounded(0xff303037.toInt(), 14)
            measure(View.MeasureSpec.makeMeasureSpec(dp(116), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(dp(48), View.MeasureSpec.EXACTLY))
            layout(0, 0, measuredWidth, measuredHeight)
        }
        return object : View.DragShadowBuilder(label) {
            override fun onDrawShadow(canvas: Canvas) = label.draw(canvas)
        }
    }

    private fun imageDragShadow(bitmap: Bitmap): View.DragShadowBuilder {
        val size = previewSize(bitmap, dp(144), dp(104))
        val radius = minOf(dp(CARD_CORNER_DP).toFloat(), minOf(size.x, size.y) / 2f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply {
                    setScale(size.x.toFloat() / bitmap.width, size.y.toFloat() / bitmap.height)
                })
            }
        }
        return object : View.DragShadowBuilder() {
            override fun onProvideShadowMetrics(shadowSize: Point, touchPoint: Point) {
                shadowSize.set(size.x, size.y)
                touchPoint.set(size.x / 2, size.y / 2)
            }

            override fun onDrawShadow(canvas: Canvas) {
                canvas.drawRoundRect(0f, 0f, size.x.toFloat(), size.y.toFloat(), radius, radius, paint)
            }
        }
    }

    private fun finishOutbound() {
        outboundWindow = null
        TransferDockDragGuard.setDragging(false)
    }

    private fun copy(item: TransferDockItem) {
        val clip = clipFor(item, forDrag = false) ?: return
        try {
            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
            Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not copy transfer dock item", e)
            Toast.makeText(context, "复制失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clipFor(item: TransferDockItem, forDrag: Boolean): ClipData? {
        val uri = if (item.type == TransferDockItem.TYPE_TEXT) null else dockItemUri(item)
        if (uri == null && item.type != TransferDockItem.TYPE_TEXT) {
            Toast.makeText(context, "文件不可用", Toast.LENGTH_SHORT).show()
            return null
        }
        val types = if (uri == null) {
            mutableListOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
        } else {
            mutableListOf(item.mimeType.ifBlank { "application/octet-stream" }, ClipDescription.MIMETYPE_TEXT_URILIST)
        }
        if (forDrag) types.add(TransferDockContract.DRAG_MIME)
        return ClipData(ClipDescription(item.title, types.toTypedArray()),
            if (uri == null) ClipData.Item(item.text ?: item.title) else ClipData.Item(uri))
    }

    private fun canShow() = power.isInteractive && !keyguard.isKeyguardLocked

    private fun render() {
        val text = if (!enabled || !canShow()) null else when {
            incoming && hovering -> "松手存入"
            incoming -> "拖拽到此处"
            saving -> "正在存入"
            saved -> "保存成功"
            items.isNotEmpty() -> "共${items.size}项"
            else -> null
        }
        nativeIsland.show(text)
    }

    private fun label(uiContext: Context, text: String, size: Float) = TextView(uiContext).apply {
        this.text = text
        textSize = size
        setTextColor(Color.WHITE)
        ellipsize = TextUtils.TruncateAt.END
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (context.resources.displayMetrics.density * value + 0.5f).toInt()

    companion object {
        private const val CARD_CORNER_DP = 13
        private const val TAG = "TransferDockIsland"
        private const val PORTAL_MIME = "application/x-dragshare"
    }
}
