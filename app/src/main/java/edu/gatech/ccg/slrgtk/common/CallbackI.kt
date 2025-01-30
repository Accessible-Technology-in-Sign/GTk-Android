package edu.gatech.ccg.slrgtk.common

import android.util.Log

interface CallbackI<T> {
    fun addCallback(name: String, callback: (T) -> Unit)
    fun removeCallback(name: String)
    fun triggerCallbacks(value: T)
    fun clearCallbacks()
}

open class CallbackManager<T> : CallbackI<T> {
    private val callbacks: MutableMap<String, (T) -> Unit> = mutableMapOf()

    override fun addCallback(name: String, callback: (T) -> Unit) {
        callbacks[name] = callback
    }

    override fun removeCallback(name: String) {
        callbacks.remove(name)
    }

    override fun triggerCallbacks(value: T) {
        callbacks.values.forEach { it(value) }
    }

    override fun clearCallbacks() {
        callbacks.clear()
    }
}
