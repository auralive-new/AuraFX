package com.aurafx.sdk.gift

/**
 * GPU gift compositor. GL objects are created on attach; [dispose] is frame-safe
 * to call once from onDetach.
 */
class GiftFxRenderer {
    @Volatile var attached: Boolean = false
        private set
    @Volatile var disposed: Boolean = false
        private set
    var lastDrawCount: Int = 0
    var freezeValid: Boolean = false

    fun onAttach() {
        attached = true
        disposed = false
        lastDrawCount = 0
        freezeValid = false
    }

    fun resetCpu() {
        lastDrawCount = 0
        freezeValid = false
    }

    fun markFreezeCaptured() {
        freezeValid = true
    }

    fun dispose() {
        attached = false
        disposed = true
        lastDrawCount = 0
        freezeValid = false
    }
}
