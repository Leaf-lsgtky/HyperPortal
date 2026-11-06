// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit

object HyperPortalPreferences {
    const val ACTION_CONFIG_CHANGED = "com.github.leaf.hyperportal.ACTION_CONFIG_CHANGED"
    const val PREFS_NAME = "hyperportal_config"

    const val KEY_MASTER_SWITCH = "master_switch"
    const val DEFAULT_MASTER_SWITCH = true

    const val KEY_LANDSCAPE_TRIGGER = "landscape_trigger"
    const val DEFAULT_LANDSCAPE_TRIGGER = false

    const val KEY_EDGE_SCROLL_MARGIN = "edge_scroll_margin"
    const val DEFAULT_EDGE_SCROLL_MARGIN = 86
    const val MIN_EDGE_SCROLL_MARGIN = 40
    const val MAX_EDGE_SCROLL_MARGIN = 140

    const val KEY_LONG_PRESS_DURATION = "long_press_duration"
    const val DEFAULT_LONG_PRESS_DURATION = 800
    const val MIN_LONG_PRESS_DURATION = 200
    const val MAX_LONG_PRESS_DURATION = 1500

    const val KEY_TOUCH_SLOP = "touch_slop"
    const val DEFAULT_TOUCH_SLOP = 24
    const val MIN_TOUCH_SLOP = 8
    const val MAX_TOUCH_SLOP = 48

    /**
     * Drop guard: when a HyperPortal drag is released over the very window it
     * started from, end the drag as "not consumed" instead of letting the app
     * swallow ACTION_DROP (WeChat would otherwise send the message the shadow
     * was made from). Different windows of the same app stay valid targets.
     */
    const val KEY_SAME_WINDOW_GUARD = "same_window_guard"
    const val DEFAULT_SAME_WINDOW_GUARD = true

    const val KEY_TRIGGER_THRESHOLD = "trigger_threshold"
    const val DEFAULT_TRIGGER_THRESHOLD = 76
    const val MIN_TRIGGER_THRESHOLD = 50
    const val MAX_TRIGGER_THRESHOLD = 95

    const val KEY_TRIGGER_POSITION = "trigger_position"
    const val POSITION_BOTTOM = "bottom"
    const val POSITION_RIGHT = "right"
    const val DEFAULT_TRIGGER_POSITION = POSITION_BOTTOM

    const val KEY_BLACKLIST = "gesture_blacklist"

    const val KEY_PRELOAD_TEXT_SEGMENTER = "preload_text_segmenter"
    const val DEFAULT_PRELOAD_TEXT_SEGMENTER = true

    const val KEY_TARGET_LIMIT = "target_limit"
    const val DEFAULT_TARGET_LIMIT = 15
    const val MIN_TARGET_LIMIT = 3
    const val MAX_TARGET_LIMIT = 24

    const val KEY_TARGET_PINNED = "target_pinned"
    const val KEY_TARGET_ORDER = "target_order"
    const val KEY_TARGET_HIDDEN = "target_hidden"
    const val KEY_DEDUPLICATE_APP_TARGETS = "deduplicate_app_targets"
    const val DEFAULT_DEDUPLICATE_APP_TARGETS = true
    const val KEY_ENABLE_QR_SCAN = "enable_qr_scan"
    const val DEFAULT_ENABLE_QR_SCAN = true
    const val KEY_TARGET_USAGE_PREFIX = "target_usage_"

    private const val KEY_PREFS_VERSION = "prefs_schema_version"
    private const val CURRENT_PREFS_VERSION = 2

    val DEFAULT_PINNED_TARGETS = listOf(
        "builtin:copy",
        "builtin:save",
        "builtin:segment",
        "builtin:share",
        "builtin:search",
    )

    private fun ensureMigrated(prefs: SharedPreferences) {
        val version = prefs.getInt(KEY_PREFS_VERSION, 1)
        if (version < CURRENT_PREFS_VERSION) {
            val pinned = prefs.getStringSet(KEY_TARGET_PINNED, null)?.toMutableSet()
            val hidden = prefs.getStringSet(KEY_TARGET_HIDDEN, emptySet())?.toSet() ?: emptySet()
            if (pinned != null) {
                if (!pinned.contains("builtin:share") && !hidden.contains("builtin:share")) {
                    pinned.add("builtin:share")
                }
                val orderText = prefs.getString(KEY_TARGET_ORDER, null)
                val order = if (orderText != null) {
                    orderText.split('\u0001').filter { it.isNotBlank() }.toMutableList()
                } else {
                    DEFAULT_PINNED_TARGETS.toMutableList()
                }
                if (!order.contains("builtin:share") && !hidden.contains("builtin:share")) {
                    val searchIdx = order.indexOf("builtin:search")
                    if (searchIdx >= 0) {
                        order.add(searchIdx, "builtin:share")
                    } else {
                        order.add("builtin:share")
                    }
                }
                prefs.edit {
                    putStringSet(KEY_TARGET_PINNED, pinned)
                    putString(KEY_TARGET_ORDER, order.joinToString("\u0001"))
                    putInt(KEY_PREFS_VERSION, CURRENT_PREFS_VERSION)
                }
            } else {
                prefs.edit { putInt(KEY_PREFS_VERSION, CURRENT_PREFS_VERSION) }
            }
        }
    }

    data class Config(
        val masterSwitch: Boolean,
        val landscapeTrigger: Boolean,
        val edgeScrollMargin: Int,
        val longPressDuration: Int,
        val touchSlop: Int,
        val sameWindowGuard: Boolean,
        val triggerThreshold: Int,
        val triggerPosition: String,
        val blacklist: Set<String>,
        val preloadTextSegmenter: Boolean,
        val targetLimit: Int,
        val pinnedTargetKeys: Set<String>,
        val pinnedTargetOrder: List<String>,
        val hiddenTargetKeys: Set<String>,
        val deduplicateAppTargets: Boolean,
        val enableQrScan: Boolean,
        val targetUsage: Map<String, Int>,
    )

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getConfig(context: Context): Config {
        val prefs = getPrefs(context)
        ensureMigrated(prefs)
        val pinned = prefs.getStringSet(KEY_TARGET_PINNED, DEFAULT_PINNED_TARGETS.toSet())
            ?.toSet() ?: DEFAULT_PINNED_TARGETS.toSet()
        val hidden = prefs.getStringSet(KEY_TARGET_HIDDEN, emptySet())
            ?.toSet() ?: emptySet()
        val orderText = prefs.getString(KEY_TARGET_ORDER, null)
        val order = if (orderText == null) {
            DEFAULT_PINNED_TARGETS
        } else {
            orderText.split('\u0001').filter { it.isNotBlank() }
        }
        val enableQrScan = prefs.getBoolean(KEY_ENABLE_QR_SCAN, DEFAULT_ENABLE_QR_SCAN)
        val usage = buildMap {
            for ((key, value) in prefs.all) {
                if (key.startsWith(KEY_TARGET_USAGE_PREFIX) && value is Int) {
                    put(key.removePrefix(KEY_TARGET_USAGE_PREFIX), value)
                }
            }
        }
        return Config(
            masterSwitch = prefs.getBoolean(KEY_MASTER_SWITCH, DEFAULT_MASTER_SWITCH),
            landscapeTrigger = prefs.getBoolean(KEY_LANDSCAPE_TRIGGER, DEFAULT_LANDSCAPE_TRIGGER),
            edgeScrollMargin = prefs.getInt(KEY_EDGE_SCROLL_MARGIN, DEFAULT_EDGE_SCROLL_MARGIN),
            longPressDuration = prefs.getInt(KEY_LONG_PRESS_DURATION, DEFAULT_LONG_PRESS_DURATION),
            touchSlop = prefs.getInt(KEY_TOUCH_SLOP, DEFAULT_TOUCH_SLOP),
            sameWindowGuard = prefs.getBoolean(KEY_SAME_WINDOW_GUARD, DEFAULT_SAME_WINDOW_GUARD),
            triggerThreshold = prefs.getInt(KEY_TRIGGER_THRESHOLD, DEFAULT_TRIGGER_THRESHOLD),
            triggerPosition = prefs.getString(KEY_TRIGGER_POSITION, DEFAULT_TRIGGER_POSITION) ?: DEFAULT_TRIGGER_POSITION,
            blacklist = prefs.getStringSet(KEY_BLACKLIST, emptySet())?.toSet() ?: emptySet(),
            preloadTextSegmenter = prefs.getBoolean(
                KEY_PRELOAD_TEXT_SEGMENTER,
                DEFAULT_PRELOAD_TEXT_SEGMENTER,
            ),
            targetLimit = prefs.getInt(KEY_TARGET_LIMIT, DEFAULT_TARGET_LIMIT)
                .coerceIn(MIN_TARGET_LIMIT, MAX_TARGET_LIMIT),
            pinnedTargetKeys = pinned,
            pinnedTargetOrder = order,
            hiddenTargetKeys = hidden,
            deduplicateAppTargets = prefs.getBoolean(
                KEY_DEDUPLICATE_APP_TARGETS,
                DEFAULT_DEDUPLICATE_APP_TARGETS,
            ),
            enableQrScan = enableQrScan,
            targetUsage = usage,
        )
    }

    fun setEnableQrScan(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_ENABLE_QR_SCAN, enabled) }
        notifyChanged(context)
    }

    fun setMasterSwitch(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_MASTER_SWITCH, enabled) }
        notifyChanged(context)
    }

    fun setLandscapeTrigger(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_LANDSCAPE_TRIGGER, enabled) }
        notifyChanged(context)
    }

    fun setEdgeScrollMargin(context: Context, marginDp: Int) {
        val clamped = marginDp.coerceIn(MIN_EDGE_SCROLL_MARGIN, MAX_EDGE_SCROLL_MARGIN)
        getPrefs(context).edit { putInt(KEY_EDGE_SCROLL_MARGIN, clamped) }
        notifyChanged(context)
    }

    fun setLongPressDuration(context: Context, duration: Int) {
        val clamped = duration.coerceIn(MIN_LONG_PRESS_DURATION, MAX_LONG_PRESS_DURATION)
        getPrefs(context).edit { putInt(KEY_LONG_PRESS_DURATION, clamped) }
        notifyChanged(context)
    }

    fun setTouchSlop(context: Context, slop: Int) {
        val clamped = slop.coerceIn(MIN_TOUCH_SLOP, MAX_TOUCH_SLOP)
        getPrefs(context).edit { putInt(KEY_TOUCH_SLOP, clamped) }
        notifyChanged(context)
    }

    fun setSameWindowGuard(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SAME_WINDOW_GUARD, enabled) }
        notifyChanged(context)
    }

    fun setTriggerThreshold(context: Context, threshold: Int) {
        val clamped = threshold.coerceIn(MIN_TRIGGER_THRESHOLD, MAX_TRIGGER_THRESHOLD)
        getPrefs(context).edit { putInt(KEY_TRIGGER_THRESHOLD, clamped) }
        notifyChanged(context)
    }

    fun setTriggerPosition(context: Context, position: String) {
        val validPosition = if (position == POSITION_RIGHT) POSITION_RIGHT else POSITION_BOTTOM
        getPrefs(context).edit { putString(KEY_TRIGGER_POSITION, validPosition) }
        notifyChanged(context)
    }

    fun setBlacklist(context: Context, blacklist: Set<String>) {
        getPrefs(context).edit { putStringSet(KEY_BLACKLIST, blacklist) }
        notifyChanged(context)
    }

    fun setPreloadTextSegmenter(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_PRELOAD_TEXT_SEGMENTER, enabled) }
        notifyChanged(context)
    }

    fun setTargetLimit(context: Context, limit: Int) {
        val clamped = limit.coerceIn(MIN_TARGET_LIMIT, MAX_TARGET_LIMIT)
        getPrefs(context).edit { putInt(KEY_TARGET_LIMIT, clamped) }
        notifyChanged(context)
    }

    fun setTargetPinned(context: Context, key: String, pinned: Boolean) {
        val config = getConfig(context)
        val pinnedKeys = config.pinnedTargetKeys.toMutableSet()
        val order = config.pinnedTargetOrder.toMutableList()
        val hiddenKeys = config.hiddenTargetKeys.toMutableSet()
        if (pinned) {
            pinnedKeys.add(key)
            hiddenKeys.remove(key)
            if (!order.contains(key)) {
                order.add(key)
            }
        } else {
            pinnedKeys.remove(key)
            order.remove(key)
        }
        getPrefs(context).edit {
            putStringSet(KEY_TARGET_PINNED, pinnedKeys)
            putStringSet(KEY_TARGET_HIDDEN, hiddenKeys)
            putString(KEY_TARGET_ORDER, order.joinToString("\u0001"))
        }
        notifyChanged(context)
    }

    fun setTargetVisible(context: Context, key: String, visible: Boolean) {
        val config = getConfig(context)
        val hidden = config.hiddenTargetKeys.toMutableSet()
        val pinned = config.pinnedTargetKeys.toMutableSet()
        val order = config.pinnedTargetOrder.toMutableList()
        if (visible) {
            hidden.remove(key)
        } else {
            hidden.add(key)
            pinned.remove(key)
            order.remove(key)
        }
        getPrefs(context).edit {
            putStringSet(KEY_TARGET_HIDDEN, hidden)
            putStringSet(KEY_TARGET_PINNED, pinned)
            putString(KEY_TARGET_ORDER, order.joinToString("\u0001"))
        }
        notifyChanged(context)
    }

    fun setPackageVisible(context: Context, keys: Collection<String>, visible: Boolean) {
        val config = getConfig(context)
        val hidden = config.hiddenTargetKeys.toMutableSet()
        val pinned = config.pinnedTargetKeys.toMutableSet()
        val order = config.pinnedTargetOrder.toMutableList()
        for (key in keys) {
            if (visible) {
                hidden.remove(key)
            } else {
                hidden.add(key)
                pinned.remove(key)
                order.remove(key)
            }
        }
        getPrefs(context).edit {
            putStringSet(KEY_TARGET_HIDDEN, hidden)
            putStringSet(KEY_TARGET_PINNED, pinned)
            putString(KEY_TARGET_ORDER, order.joinToString("\u0001"))
        }
        notifyChanged(context)
    }

    fun setPinnedTargetOrder(context: Context, order: List<String>) {
        val config = getConfig(context)
        val pinned = config.pinnedTargetKeys
        val filtered = order.filter { pinned.contains(it) }
        getPrefs(context).edit {
            putString(KEY_TARGET_ORDER, filtered.joinToString("\u0001"))
        }
        notifyChanged(context)
    }

    fun recordTargetUse(context: Context, key: String) {
        if (key.isBlank()) return
        val prefs = getPrefs(context)
        val storageKey = KEY_TARGET_USAGE_PREFIX + key
        val next = (prefs.getInt(storageKey, 0) + 1).coerceAtMost(1000)
        prefs.edit { putInt(storageKey, next) }
    }

    fun setDeduplicateAppTargets(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_DEDUPLICATE_APP_TARGETS, enabled) }
        notifyChanged(context)
    }

    fun toggleBlacklistPackage(context: Context, packageName: String): Boolean {
        val current = getConfig(context).blacklist.toMutableSet()
        val isAdded: Boolean
        if (current.contains(packageName)) {
            current.remove(packageName)
            isAdded = false
        } else {
            current.add(packageName)
            isAdded = true
        }
        setBlacklist(context, current)
        return isAdded
    }

    private fun notifyChanged(context: Context) {
        try {
            val cfg = getConfig(context)
            val intent = Intent(ACTION_CONFIG_CHANGED)
            intent.putExtra(KEY_MASTER_SWITCH, cfg.masterSwitch)
            intent.putExtra(KEY_LANDSCAPE_TRIGGER, cfg.landscapeTrigger)
            intent.putExtra(KEY_EDGE_SCROLL_MARGIN, cfg.edgeScrollMargin)
            intent.putExtra(KEY_LONG_PRESS_DURATION, cfg.longPressDuration)
            intent.putExtra(KEY_TOUCH_SLOP, cfg.touchSlop)
            intent.putExtra(KEY_SAME_WINDOW_GUARD, cfg.sameWindowGuard)
            intent.putExtra(KEY_TRIGGER_THRESHOLD, cfg.triggerThreshold)
            intent.putExtra(KEY_TRIGGER_POSITION, cfg.triggerPosition)
            intent.putExtra(KEY_TARGET_LIMIT, cfg.targetLimit)
            intent.putExtra(KEY_DEDUPLICATE_APP_TARGETS, cfg.deduplicateAppTargets)
            intent.putExtra(KEY_ENABLE_QR_SCAN, cfg.enableQrScan)
            context.sendBroadcast(intent)
        } catch (_: Throwable) {
        }
    }
}
