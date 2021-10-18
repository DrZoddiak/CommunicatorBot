package io.github.divinegenesis.communicator.utils

import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse


fun User.sendPrivateMessage(context: TextChannel, content: String) {
    this.openPrivateChannel()
        .flatMap { it.sendMessage(content) }
        .queue(null, ErrorHandler()
            .handle(ErrorResponse.CANNOT_SEND_TO_USER) {
                context.sendMessage("You have private messages disabled, ${this.asTag}").queue()
            }
        )
}
