package net.lostillusion.frameworks.javacordcustomevents

interface CustomEventListener<T: CustomEvent<*>> {
    fun onEvent(event: T)
}
