package net.lostillusion.frameworks.javacordcustomevents

import org.javacord.api.event.Event

abstract class CustomEventDispatcher<I: Event, O: CustomEvent<out I>>(val inputEventClass: Class<I>) {
    private val eventListeners: MutableMap<String, CustomEventListener<O>> = mutableMapOf()
    protected abstract val eventFilter: (event: I) -> Boolean
    protected abstract val eventTransformer: (event: I) -> O

    fun dispatchNormalEvent(event: I) {
        if(eventFilter.invoke(event))
            dispatchCustomEvent(eventTransformer.invoke(event))
    }

    private fun dispatchCustomEvent(event: O) =
        eventListeners.values.forEach { it.onEvent(event) }

    fun addListener(name: String, listener: CustomEventListener<O>) =
        eventListeners.put(name, listener)
}
