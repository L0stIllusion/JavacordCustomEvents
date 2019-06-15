package net.lostillusion.frameworks.javacordcustomevents

import net.lostillusion.frameworks.javacordcustomevents.CustomEvent

interface CustomEventListener<T: CustomEvent<*>> {
    fun onEvent(event: T)
}
