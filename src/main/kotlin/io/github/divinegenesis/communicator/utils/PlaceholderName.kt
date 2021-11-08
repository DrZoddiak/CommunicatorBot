package io.github.divinegenesis.communicator.utils

import io.github.divinegenesis.communicator.config.AuthorizationConfig
import io.github.divinegenesis.communicator.events.tables.UserTransaction
import net.dv8tion.jda.api.entities.TextChannel

suspend fun parseReactions(channel: TextChannel?, content: String, authorizationConfig: AuthorizationConfig) {
    val guild = channel?.guild
    val message = channel?.retrieveMessageById(authorizationConfig.messageID)?.await()
    val emote = guild?.getEmoteById(authorizationConfig.emoteID)

    val reactionUsers = emote?.let { message?.retrieveReactionUsers(it)?.await() }

    reactionUsers?.forEach { user ->
        val transaction = UserTransaction.getOrCreate(user)

        if (!user.isInGuild(guild)) {
            message?.removeReaction(emote, user)?.queue()
            transaction.setReacted(false) //Redundant in most scenarios
            return@forEach
        }

        transaction.setReacted(true)

        if (user.wasProcessed()) return@forEach
        if (user.isProcessing()) return@forEach

        user.sendPrivateMessage(
            channel, content
        )
    }
}