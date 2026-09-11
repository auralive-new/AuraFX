package com.aurafx.sdk.effect

import com.aurafx.sdk.internal.AuraFxLog
import com.aurafx.sdk.performance.PerformanceManager
import com.aurafx.sdk.vision.TrackingData
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Ordered effect graph. With zero effects the pipeline is identity (camera blit only).
 */
class EffectManager(
    private val performance: PerformanceManager,
) {
    private val effects = CopyOnWriteArrayList<Effect>()
    @Volatile private var attachedContext: EffectContext? = null

    fun registeredIds(): List<String> = effects.map { it.id }

    fun register(effect: Effect) {
        val ctx = attachedContext
        if (ctx != null) {
            val started = System.nanoTime()
            effect.onAttach(ctx)
            performance.markEffectLoadNs(System.nanoTime() - started)
        }
        effects.add(effect)
    }

    fun unregister(id: String) {
        val iterator = effects.iterator()
        while (iterator.hasNext()) {
            val effect = iterator.next()
            if (effect.id == id) {
                effect.onDetach()
                effects.remove(effect)
                break
            }
        }
    }

    internal fun attach(context: EffectContext) {
        attachedContext = context
        for (effect in effects) {
            val started = System.nanoTime()
            try {
                effect.onAttach(context)
            } catch (t: Throwable) {
                AuraFxLog.e("effect attach failed ${effect.id}", t)
            }
            performance.markEffectLoadNs(System.nanoTime() - started)
        }
    }

    internal fun detachAll() {
        for (effect in effects) {
            effect.onDetach()
        }
        attachedContext = null
    }

    internal fun process(frame: FrameContext, tracking: TrackingData) {
        for (effect in effects) {
            try {
                effect.process(frame, tracking)
            } catch (t: Throwable) {
                AuraFxLog.e("effect process failed ${effect.id}", t)
            }
        }
    }
}
