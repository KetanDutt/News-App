package com.rtctek.newsapp.utils

/**
 * Wrapper for one-shot events (snackbars, toasts, navigation) exposed through
 * LiveData, so a message is consumed exactly once even across re-observations
 * after configuration changes.
 */
class Event<out T>(private val content: T) {

    private var hasBeenHandled = false

    /** Returns the content exactly once; `null` on every subsequent call. */
    fun getIfNotHandled(): T? =
        if (hasBeenHandled) null else {
            hasBeenHandled = true
            content
        }
}
