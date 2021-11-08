package io.github.divinegenesis.communicator.utils

import io.github.divinegenesis.communicator.Communicator
import io.github.divinegenesis.communicator.events.tables.UserTransaction
import io.github.divinegenesis.communicator.logging.logger
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse.CANNOT_SEND_TO_USER

val logger = logger<Communicator>()

fun User.sendPrivateMessage(context: TextChannel?, content: String) {
    this.openPrivateChannel()
        .flatMap { it.sendMessage(content) }
        .queue(null, ErrorHandler()
            .handle(CANNOT_SEND_TO_USER) {
                logger.info("${this.asTag} has private messages disabled")
                context?.sendMessage("You have private messages disabled, ${this.asTag}")?.queue()
            }
        )
}

fun User.isInGuild(guild: Guild): Boolean {
    var isInGuild = false

    guild.retrieveMember(this).queue { isInGuild = true }

    return isInGuild
}

suspend fun User.wasProcessed(): Boolean {
    return UserTransaction.getOrCreate(this).processed
}

suspend fun User.isProcessing(): Boolean {
    return UserTransaction.getOrCreate(this).processing
}

suspend fun User.leaves(): Int {
    return UserTransaction.getOrCreate(this).leaves
}

suspend fun User.reacted(): Boolean {
    return UserTransaction.getOrCreate(this).reacted
}

suspend fun User.isSuspended(): Boolean {
    return UserTransaction.getOrCreate(this).suspended
}

suspend fun User.removedRoles(): String? {
    return UserTransaction.getOrCreate(this).roles
}
