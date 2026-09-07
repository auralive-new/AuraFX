package com.aurafx.sdk.internal.render

import android.os.Handler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal fun Handler.runSync(timeoutMs: Long = 1_500, block: () -> Unit) {
    if (looper.thread === Thread.currentThread()) {
        block()
        return
    }
    val done = CountDownLatch(1)
    var error: Throwable? = null
    val posted = post {
        try {
            block()
        } catch (t: Throwable) {
            error = t
        } finally {
            done.countDown()
        }
    }
    if (!posted) {
        throw IllegalStateException("GL handler is shutting down")
    }
    if (!done.await(timeoutMs, TimeUnit.MILLISECONDS)) {
        throw IllegalStateException("Timed out waiting for GL thread")
    }
    error?.let { throw it }
}
