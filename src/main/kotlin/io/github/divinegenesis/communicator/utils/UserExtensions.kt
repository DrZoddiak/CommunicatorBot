package io.github.divinegenesis.communicator.utils

import io.github.divinegenesis.communicator.Communicator
import io.github.divinegenesis.communicator.logging.logger
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse.*

val logger = logger<Communicator>()

fun User.sendPrivateMessage(context: TextChannel, content: String) {
    this.openPrivateChannel()
        .flatMap { it.sendMessage(content) }
        .queue(null, ErrorHandler()
            .handle(CANNOT_SEND_TO_USER) {
                context.sendMessage("You have private messages disabled, ${this.asTag}").queue()
            }
        )
}