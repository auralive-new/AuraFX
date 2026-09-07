package com.aurafx.sdk.internal.render

import android.opengl.GLES30

/**
 * Optional GPU elapsed-time query via EXT_disjoint_timer_query tokens.
 * GLES 3.0 Java bindings do not expose [GL_TIME_ELAPSED]; if the driver does not
 * support the extension, [supported] stays false and [pollNs] returns null.
 */
internal class GpuTimer {
    var supported: Boolean = false
        private set
    private val queries = IntArray(1)
    private var pending = false

    fun init() {
        val bits = IntArray(1)
        GLES30.glGetQueryiv(GL_TIME_ELAPSED, GL_QUERY_COUNTER_BITS, bits, 0)
        val err = GLES30.glGetError()
        supported = err == GLES30.GL_NO_ERROR && bits[0] > 0
        if (supported) {
            GLES30.glGenQueries(1, queries, 0)
        }
    }

    fun begin() {
        if (!supported) return
        GLES30.glBeginQuery(GL_TIME_ELAPSED, queries[0])
    }

    fun end() {
        if (!supported) return
        GLES30.glEndQuery(GL_TIME_ELAPSED)
        pending = true
    }

    fun pollNs(): Long? {
        if (!supported || !pending) return null
        val available = IntArray(1)
        GLES30.glGetQueryObjectuiv(queries[0], GLES30.GL_QUERY_RESULT_AVAILABLE, available, 0)
        if (available[0] == GLES30.GL_FALSE) return null
        val result = IntArray(1)
        GLES30.glGetQueryObjectuiv(queries[0], GLES30.GL_QUERY_RESULT, result, 0)
        pending = false
        return result[0].toLong() and 0xffffffffL
    }

    fun release() {
        if (supported && queries[0] != 0) {
            GLES30.glDeleteQueries(1, queries, 0)
            queries[0] = 0
        }
        supported = false
        pending = false
    }

    private companion object {
        const val GL_TIME_ELAPSED = 0x88BF
        const val GL_QUERY_COUNTER_BITS = 0x8864
    }
}
