package edu.gatech.ccg.slrgtk.system

import edu.gatech.ccg.slrgtk.common.CallbackManager

class DefaultFillerDefinitions<T> {
    fun passThroughFiller(): (MutableList<T>, T) -> Boolean = { internalList, frame ->
        internalList.add(frame)
        true
    }

    fun capacityFiller(capacity: Int): (MutableList<T>, T) -> Boolean = {
        internalList, frame ->
        internalList.add(frame)
        val ret = internalList.size >= capacity
        while (internalList.size > capacity) internalList.removeAt(internalList.size - 1)
        ret
    }
}

class Buffer<T> (
    val filler: (MutableList<T>, T) -> Boolean = DefaultFillerDefinitions<T>().capacityFiller(60)
) : CallbackManager<List<T>>() {
    private val internalBuffer = mutableListOf<T>()

    fun addElement(elem: T) {
        if (filler(internalBuffer, elem)) triggerCallbacks()
    }

    fun triggerCallbacks() {
        triggerCallbacks(internalBuffer.toList())
    }

    fun clear() {
        internalBuffer.clear()
    }

    val size: Int
        get() = internalBuffer.size
}