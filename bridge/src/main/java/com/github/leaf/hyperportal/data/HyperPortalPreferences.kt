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

    const val KEY_LONG_PRESS_DURATION = "long_press_duration"
    const val DEFAULT_LONG_PRESS_DURATION = 400
    const val MIN_LONG_PRESS_DURATION = 200
    const val MAX_LONG_PRESS_DURATION = 800

    const val KEY_TOUCH_SLOP = "touch_slop"
    const val DEFAULT_TOUCH_SLOP = 24
    const val MIN_TOUCH_SLOP = 8
    const val MAX_TOUCH_SLOP = 48

    const val KEY_BLACKLIST = "gesture_blacklist"

    data class Config(
        val masterSwitch: Boolean,
        val longPressDuration: Int,
        val touchSlop: Int,
        val blacklist: Set<String>,
    )

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getConfig(context: Context): Config {
        val prefs = getPrefs(context)
        return Config(
            masterSwitch = prefs.getBoolean(KEY_MASTER_SWITCH, DEFAULT_MASTER_SWITCH),
            longPressDuration = prefs.getInt(KEY_LONG_PRESS_DURATION, DEFAULT_LONG_PRESS_DURATION),
            touchSlop = prefs.getInt(KEY_TOUCH_SLOP, DEFAULT_TOUCH_SLOP),
            blacklist = prefs.getStringSet(KEY_BLACKLIST, emptySet())?.toSet() ?: emptySet(),
        )
    }

    fun setMasterSwitch(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_MASTER_SWITCH, enabled) }
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

    fun setBlacklist(context: Context, blacklist: Set<String>) {
        getPrefs(context).edit { putStringSet(KEY_BLACKLIST, blacklist) }
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
            val intent = Intent(ACTION_CONFIG_CHANGED)
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        } catch (_: Throwable) {
        }
    }
}
