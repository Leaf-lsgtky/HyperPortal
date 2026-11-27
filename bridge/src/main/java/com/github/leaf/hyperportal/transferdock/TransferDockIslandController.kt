// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock

import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.PowerManager
import android.text.TextUtils
import android.view.DragEvent
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import java.util.concurrent.Executor

internal class TransferDockIslandController(
    private val context: Context,
    private val main: Handler,
    private val worker: Executor,
    private val onDrop: (ClipData) -> Boolean,
) {
    private val windows = context.getSystemService(WindowManager::class.java)
    private val keyguard = context.getSystemService(KeyguardManager::class.java)
    private val power = context.getSystemService(PowerManager::class.java)
    private val nativeIsland = TransferDockNativeIsland(context, worker)
    private val outboundToken = Any()
    private val bounds = Rect()
    private var enabled = false
    private var incoming = false
    private var outbound = false
    private var saved = false
    private var saving = false
    private var showing = false
    private var capsuleAdded = false
    private var items = emptyList<TransferDockItem>()
    private var panel: HorizontalScrollView? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private val resetSaved = Runnable {
        saved = false
        render()
    }
    private val endIncoming = Runnable { setIncoming(false) }
    private val leadingText = label("", 14f)
    private val trailingText = label("", 14f)
    private val icon = ImageView(context).apply {
        setImageResource(android.R.drawable.ic_menu_save)
        setColorFilter(Color.WHITE)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val capsule = object : LinearLayout(context) {
        override fun onConfigurationChanged(config: android.content.res.Configuration) {
            super.onConfigurationChanged(config)
            post { render() }
        }
    }.apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(15), 0, dp(15), 0)
        addView(icon, LinearLayout.LayoutParams(dp(19), dp(19)))
        addView(leadingText, LinearLayout.LayoutParams(0, -2, 1f))
        addView(trailingText)
        setOnClickListener { if (!incoming && !outbound) togglePanel() }
        setOnLongClickListener { !incoming && items.firstOrNull()?.let { startDrag(this, it) } == true }
        setOnDragListener { _, event -> onCapsuleDrag(event) }
    }
    private val capsuleParams = params(dp(176), dp(34), "HyperPortal Transfer Dock")

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        if (enabled) refreshItems() else {
            incoming = false
            saved = false
            saving = false
            main.removeCallbacks(resetSaved)
            if (!outbound) closePanel()
        }
        render()
    }

    fun setIncoming(value: Boolean) {
        main.removeCallbacks(endIncoming)
        if (!enabled || incoming == value) return
        incoming = value
        if (incoming) closePanel()
        render()
    }

    fun finishIncomingSoon() {
        main.removeCallbacks(endIncoming)
        if (incoming) main.postDelayed(endIncoming, 100)
    }

    fun dropAt(x: Float, y: Float, clip: ClipData?): Boolean =
        bounds.contains(x.toInt(), y.toInt()) && acceptDrop(clip)

    fun onScreenChanged() {
        if (!canShow()) {
            incoming = false
            if (!outbound) closePanel()
        }
        render()
    }

    fun refreshItems(justSaved: Boolean = false) {
        worker.execute {
            val snapshot = TransferDockManager.getInstance(context).getItems()
            main.post {
                items = snapshot
                if (enabled && justSaved) {
                    saving = false
                    saved = true
                    main.removeCallbacks(resetSaved)
                    main.postDelayed(resetSaved, 1800)
                }
                panel?.let(::populatePanel)
                render()
            }
        }
    }

    fun saveFailed() {
        saving = false
        render()
    }

    fun openPanel() {
        if (!enabled || !canShow() || items.isEmpty() || panel != null) return
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            clipToOutline = true
            background = rounded(0xff17171c.toInt(), 26)
            setPadding(dp(9), dp(9), dp(9), dp(9))
            setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                    closePanel()
                    true
                } else false
            }
            setOnDragListener { _, event -> onOutboundDrag(event) }
        }
        panel = scroll
        val layout = params(panelWidth(), dp(102), "HyperPortal Transfer Dock Contents")
        layout.flags = layout.flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        layout.y = bounds.bottom + dp(6)
        panelParams = layout
        populatePanel(scroll)
        try {
            windows.addView(scroll, layout)
        } catch (e: RuntimeException) {
            panel = null
            panelParams = null
            Log.w(TAG, "Could not open transfer dock contents", e)
        }
    }

    private fun togglePanel() {
        if (panel == null) openPanel() else closePanel()
    }

    private fun populatePanel(scroll: HorizontalScrollView) {
        scroll.removeAllViews()
        val row = LinearLayout(context)
        scroll.addView(row)
        val thumbnails = mutableListOf<Pair<String, ImageView>>()
        for (item in items) {
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(5), dp(8), dp(5))
                background = rounded(0xff303037.toInt(), 14)
                contentDescription = item.title
                setOnClickListener { copy(item) }
                setOnLongClickListener { startDrag(this, item) }
            }
            val preview = ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(if (item.type == TransferDockItem.TYPE_TEXT) {
                    android.R.drawable.ic_menu_edit
                } else android.R.drawable.ic_menu_gallery)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            card.addView(preview, LinearLayout.LayoutParams(dp(34), dp(34)))
            card.addView(label(item.title, 12f).apply { gravity = Gravity.CENTER })
            row.addView(card, LinearLayout.LayoutParams(dp(88), -1).apply { marginEnd = dp(6) })
            item.thumbnailPath?.let { thumbnails.add(it to preview) }
        }
        worker.execute {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            val decoded = thumbnails.map { (path, view) -> view to BitmapFactory.decodeFile(path, options) }
            main.post {
                if (panel === scroll && scroll.getChildAt(0) === row) {
                    for ((view, bitmap) in decoded) if (bitmap != null) view.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun closePanel() {
        val view = panel ?: return
        panel = null
        panelParams = null
        try {
            windows.removeView(view)
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Transfer dock contents already removed", e)
        }
    }

    private fun onCapsuleDrag(event: DragEvent): Boolean {
        if (onOutboundDrag(event)) return true
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                if (!enabled || !canShow() || event.clipDescription?.hasMimeType(PORTAL_MIME) != true) return false
                setIncoming(true)
                return true
            }
            DragEvent.ACTION_DROP -> return acceptDrop(event.clipData)
            DragEvent.ACTION_DRAG_ENDED -> setIncoming(false)
        }
        return incoming
    }

    private fun acceptDrop(clip: ClipData?): Boolean {
        if (!incoming || !canShow() || clip == null) return false
        val accepted = onDrop(clip)
        if (accepted) saving = true
        setIncoming(false)
        return accepted
    }

    private fun onOutboundDrag(event: DragEvent): Boolean {
        if (event.localState !== outboundToken) return false
        if (event.action == DragEvent.ACTION_DRAG_ENDED) {
            outbound = false
            TransferDockDragGuard.setDragging(false)
            closePanel()
            render()
        }
        return event.action != DragEvent.ACTION_DROP
    }

    private fun startDrag(view: View, item: TransferDockItem): Boolean {
        if (outbound || !canShow()) return false
        val clip = clipFor(item, forDrag = true) ?: return false
        TransferDockDragGuard.setDragging(true)
        outbound = true
        val started = try {
            val shadowView = if (view === capsule) label(item.title, 14f).apply {
                gravity = Gravity.CENTER
                setPadding(dp(9), 0, dp(9), 0)
                background = rounded(0xff303037.toInt(), 14)
                measure(View.MeasureSpec.makeMeasureSpec(dp(116), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dp(48), View.MeasureSpec.EXACTLY))
                layout(0, 0, measuredWidth, measuredHeight)
            } else view
            view.startDragAndDrop(clip, View.DragShadowBuilder(shadowView), outboundToken,
                View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not drag transfer dock item", e)
            false
        }
        if (started) {
            panel?.let { view ->
                view.alpha = 0f
                panelParams?.let {
                    it.flags = it.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    windows.updateViewLayout(view, it)
                }
            }
            render()
        } else {
            outbound = false
            TransferDockDragGuard.setDragging(false)
        }
        return started
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
        val text = item.text ?: item.title
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
            if (uri == null) ClipData.Item(text) else ClipData.Item(uri))
    }

    fun nativeBoundsChanged() {
        if (capsuleAdded) positionCapsule()
    }

    private fun canShow() = power.isInteractive && !keyguard.isKeyguardLocked

    private fun render() {
        val dropHint = incoming || (saving && items.isEmpty())
        val publish = enabled && canShow() && (dropHint || items.isNotEmpty())
        showing = publish && !outbound
        val left = if (dropHint) "" else "中转站"
        val right = when {
            dropHint -> "中转站"
            saved -> "保存成功"
            else -> "共${items.size}项"
        }
        nativeIsland.show(if (publish) left else null, right)
        if (!enabled && !outbound) {
            if (capsuleAdded) {
                capsuleAdded = false
                windows.removeView(capsule)
            }
            return
        }
        icon.visibility = if (dropHint) View.VISIBLE else View.GONE
        leadingText.text = left
        trailingText.text = right
        capsule.contentDescription = "$left $right".trim()
        positionCapsule()
    }

    private fun positionCapsule() {
        val metrics = windows.currentWindowMetrics
        val native = nativeIsland.bounds()
        val inset = metrics.windowInsets.displayCutout?.safeInsetTop ?: 0
        val width = dp(176)
        val height = dp(34)
        val left = (metrics.bounds.width() - width) / 2
        val top = ((inset - height) / 2).coerceAtLeast(dp(3))
        bounds.set(native ?: Rect(left, top, left + width, top + height))
        capsule.background = if (native == null) rounded(Color.BLACK, 28) else null
        val contentAlpha = if (native == null) 1f else 0f
        icon.alpha = contentAlpha
        leadingText.alpha = contentAlpha
        trailingText.alpha = contentAlpha
        // When another native island takes over, give its touch region back to SystemUI.
        val interactive = showing && (incoming || saving || native != null || !nativeIsland.wasRendered())
        capsule.alpha = if (interactive) 1f else 0f
        capsuleParams.flags = if (interactive) {
            capsuleParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            capsuleParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        capsuleParams.width = bounds.width()
        capsuleParams.height = bounds.height()
        capsuleParams.x = bounds.left
        capsuleParams.y = bounds.top
        try {
            if (capsuleAdded) {
                windows.updateViewLayout(capsule, capsuleParams)
            } else {
                windows.addView(capsule, capsuleParams)
                capsuleAdded = true
            }
            val scroll = panel
            val layout = panelParams
            if (scroll != null && layout != null && scroll.isAttachedToWindow) {
                layout.width = panelWidth()
                layout.y = bounds.bottom + dp(6)
                windows.updateViewLayout(scroll, layout)
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not place transfer dock island", e)
        }
    }

    private fun panelWidth() = minOf(dp(360), windows.currentWindowMetrics.bounds.width() - dp(24))
    private fun label(text: String, size: Float) = TextView(context).apply {
        this.text = text
        textSize = size
        setTextColor(Color.WHITE)
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.END
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (context.resources.displayMetrics.density * value + 0.5f).toInt()

    private fun params(width: Int, height: Int, name: String) = WindowManager.LayoutParams(
        width, height, 2024,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        if (name == "HyperPortal Transfer Dock") gravity = Gravity.TOP or Gravity.LEFT
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        setFitInsetsTypes(0)
        title = name
    }

    companion object {
        private const val TAG = "TransferDockIsland"
        private const val PORTAL_MIME = "application/x-dragshare"
    }
}
