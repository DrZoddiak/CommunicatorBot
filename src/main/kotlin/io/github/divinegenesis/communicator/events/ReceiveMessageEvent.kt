package io.github.divinegenesis.communicator.events

import io.github.divinegenesis.communicator.logging.logger
import io.github.divinegenesis.communicator.utils.handleEachIn
import io.github.divinegenesis.communicator.utils.listenFlow
import io.github.divinegenesis.communicator.utils.scope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class ReceiveMessageEvent : EventListener {

    private fun onPrivateMessageReceived(event: MessageReceivedEvent) {
        val channel = event.channel
        logger<ReceiveMessageEvent>().info(event.message.contentRaw)
    }

    override fun register(jda: JDA) {
        jda.listenFlow<MessageReceivedEvent>().handleEachIn(scope, this::onPrivateMessageReceived)
    }
}