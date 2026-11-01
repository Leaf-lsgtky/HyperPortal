// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal

import android.app.Application
import com.github.leaf.hyperportal.bigbang.TextSegmenter

class HyperPortalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TextSegmenter.preloadIfEnabled(this)
    }
}
