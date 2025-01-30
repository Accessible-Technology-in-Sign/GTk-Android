package edu.gatech.ccg.slrgtk.common

interface FilterI<T> {
    fun addFilter(name: String, filter: (T) -> T)
    fun removeFilter(name: String)
    fun filter(value: T): T
    fun clearCallbacks()
}

open class FilterManager<T> : FilterI<T> {
    private val filters: MutableMap<String, (T) -> T> = mutableMapOf()

    override fun addFilter(name: String, filter: (T) -> T) {
        filters[name] = filter
    }

    override fun removeFilter(name: String) {
        filters.remove(name)
    }

    override fun filter(value: T): T {
        var currentValue = value
        for (filter in filters) {
            currentValue = filter.value(currentValue)
        }
        return currentValue
    }

    override fun clearCallbacks() {
        filters.clear()
    }
}

