package com.github.leaf.hyperportal.bigbang

import android.util.Log

import android.content.Context

/**
 * Values consumed by the imported BigBang word-chip core. DragShare does not expose the
 * reference app's separate search-settings surface, so the original defaults are retained.
 */
internal class BigBangSettings private constructor() {
    val gapRowHeightPercent: Int
        get() = 100

    val webSearchType: Int
        get() = BoomActionHandler.SEARCH_WEB

    val dictSearchType: Int
        get() = BoomActionHandler.SEARCH_DICTIONARY

    companion object {
        private val INSTANCE = BigBangSettings()

        fun get(context: Context?): BigBangSettings = INSTANCE
    }
}
