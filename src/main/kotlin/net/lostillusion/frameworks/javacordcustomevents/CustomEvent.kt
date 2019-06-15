package net.lostillusion.frameworks.javacordcustomevents

import org.javacord.api.DiscordApi
import org.javacord.api.event.Event

abstract class CustomEvent<I: Event>(@Suppress("unused") val event: I): Event {
    override fun getApi(): DiscordApi =
        CustomEvents.api
}
