package com.github.leaf.hyperportal.bigbang

import android.util.Log

/** DragShare captures one text block at a time, so BigBang's adjacent-paragraph pull is inert. */
internal class TextSessionCoordinator private constructor() {
    fun peekAdjacentText(direction: String?): String? = null

    companion object {
        val INSTANCE = TextSessionCoordinator()
    }
}
