package io.github.divinegenesis.communicator.events

import net.dv8tion.jda.api.JDA

interface EventListener {
    fun register(jda: JDA)
}