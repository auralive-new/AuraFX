package com.aurafx.sdk.api

sealed class AuraFxResult<out T> {
    data class Ok<T>(val value: T) : AuraFxResult<T>()
    data class Err(val error: AuraFxError) : AuraFxResult<Nothing>()

    val isOk: Boolean get() = this is Ok
    val isErr: Boolean get() = this is Err

    fun errorOrNull(): AuraFxError? = (this as? Err)?.error
    fun getOrNull(): T? = (this as? Ok)?.value
}

inline fun <T> AuraFxResult<T>.onOk(block: (T) -> Unit): AuraFxResult<T> {
    if (this is AuraFxResult.Ok) block(value)
    return this
}

inline fun <T> AuraFxResult<T>.onErr(block: (AuraFxError) -> Unit): AuraFxResult<T> {
    if (this is AuraFxResult.Err) block(error)
    return this
}
