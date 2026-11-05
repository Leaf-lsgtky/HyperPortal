// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal

import android.app.Application
import com.github.leaf.hyperportal.bigbang.TextSegmenter
import com.github.leaf.hyperportal.data.LsposedStatus

class HyperPortalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Bound here rather than in the activity: the service is how the home page learns which
        // processes the module is injected into, and binding it once per process means the
        // answer is already there when the page is first composed.
        LsposedStatus.init(this)
        TextSegmenter.preloadIfEnabled(this)
    }
}
