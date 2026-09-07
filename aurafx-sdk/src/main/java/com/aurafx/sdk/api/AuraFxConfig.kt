package com.aurafx.sdk.api

/**
 * Process-wide SDK configuration. Applied at [com.aurafx.sdk.AuraFx.initialize].
 */
data class AuraFxConfig(
    val enablePerformanceInstrumentation: Boolean = true,
    val requireGles3: Boolean = true,
)
