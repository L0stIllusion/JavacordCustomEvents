package net.lostillusion.frameworks.javacordcustomevents

import org.javacord.api.DiscordApi
import org.javacord.api.event.Event
import org.javacord.api.listener.GloballyAttachableListener

class CustomEvents {
    companion object {
        lateinit var api: DiscordApi
        @PublishedApi
        internal val dispatchers: MutableList<CustomEventDispatcher<*, *>> = mutableListOf()

        fun registerApi(api: DiscordApi) = apply {
            this.api = api
        }

        fun addListener(listener: GloballyAttachableListener) = apply {
            api.addListener(listener)
        }

        inline fun <reified I : Event, reified O : CustomEvent<I>, reified L : GloballyAttachableListener> registerDispatcherAndListener(
            dispatcher: CustomEventDispatcher<I, O>,
            listenerName: String,
            listener: CustomEventListener<O>,
            baseListener: Class<L>
        ) = apply {
            registerDispatcher(I::class.java, baseListener, dispatcher)
            addCustomListener(listenerName, listener, I::class.java)
        }

        inline fun <reified BaseListener : GloballyAttachableListener, reified I : Event, O : CustomEvent<I>> registerDispatcher(
            eventClass: Class<I>,
            listenerClass: Class<BaseListener>,
            dispatcher: CustomEventDispatcher<I, O>
        ) = apply {
            if (dispatchers.none { it.inputEventClass == I::class.java })
                api.addListener(interceptorMap[listenerClass]!!)
            dispatchers.add(dispatcher)
        }

        @Suppress("UNCHECKED_CAST")
        fun <I : Event, T : CustomEvent<I>> addCustomListener(
            name: String,
            listener: CustomEventListener<T>,
            eventClass: Class<I>
        ) = apply {
            dispatchers
                .filter { it.inputEventClass == eventClass }
                .mapNotNull { it as? CustomEventDispatcher<I, T> }
                .forEach { it.addListener(name, listener) }
        }
    }
}
