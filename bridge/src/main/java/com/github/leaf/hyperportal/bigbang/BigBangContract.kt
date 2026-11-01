// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.bigbang

import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Cross-process launch contract for the BigBang text-segmentation surface.
 *
 * The SystemUI LSPosed module runs under com.android.systemui's UID, so it must not
 * reference [TextSegmentationActivity] directly (that would load the app's Activity and
 * Compose classes into the SystemUI process). It also cannot use its own package name
 * when building the component name, hence the hard-coded target package below.
 */
object BigBangContract {
    private const val TARGET_PACKAGE = "com.github.leaf.hyperportal"
    private const val ACTIVITY_CLASS =
        "com.github.leaf.hyperportal.bigbang.TextSegmentationActivity"

    const val EXTRA_TOUCH_X = "bigbang_touch_x"
    const val EXTRA_TOUCH_Y = "bigbang_touch_y"

    @JvmStatic
    fun createIntent(context: Context, text: String, touchX: Int, touchY: Int): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            component = ComponentName(TARGET_PACKAGE, ACTIVITY_CLASS)
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(EXTRA_TOUCH_X, touchX)
            putExtra(EXTRA_TOUCH_Y, touchY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
    }
}
