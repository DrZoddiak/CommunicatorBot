package io.github.divinegenesis.communicator.events

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.inject.Inject
import io.github.divinegenesis.communicator.config.ConfigManager
import io.github.divinegenesis.communicator.logging.logger
import io.github.divinegenesis.communicator.utils.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent


class AuthorizationHandler @Inject constructor(configManager: ConfigManager) : EventListener {

    private val config = configManager.config
    private val mainConfiguration = config.mainConfiguration
    private val authorizationConfig = config.authorizationConfig
    private val logger = logger<AuthorizationHandler>()

    private fun onGuildEmoteReact(event: GuildMessageReactionAddEvent) {
        //This implementation doesn't support discord Emojis
        //Only supports custom emotes.
        val emote = event.reactionEmote.let {
            if (it.isEmote) return@let it.id
            else return
        }
        val channel = event.channel

        if (emote != authorizationConfig.emoteID) return
        if (channel.id != authorizationConfig.channelID) return

        val user = event.user

        /**
         * @see io.github.divinegenesis.communicator.utils.sendPrivateMessage
         */
        user.sendPrivateMessage(
            channel, """
            *Greetings, my dear Anonimian.*
            *You will be authorized after you answer three questions, __understood?__*
        """.trimIndent()
        )
    }

    private val messageCache = Caffeine.newBuilder()
        .build<String, CacheHolder>()

    private fun onPrivateMessageReceived(event: MessageReceivedEvent) {
        if (event.channelType != ChannelType.PRIVATE) return

        val guild = event.jda.getGuildById(mainConfiguration.guildID)!! //TODO: Handle this
        val authChannel = guild.getTextChannelById(authorizationConfig.channelID)!!

        val message = event.message.contentRaw
        val user = event.author
        val userById = user.id

        val cache =
            messageCache.getIfPresent(userById).let {
                if (it != null) {
                    it
                } else {
                    messageCache.put(userById, CacheHolder(mutableListOf(message), 0))
                    messageCache.getIfPresent(userById)!!
                }
            }

        val messageList = cache.messageList
        var messageInt = cache.messageInt
        val questions = authorizationConfig.questions


        when (messageInt) {
            0,1,2 -> {
                messageCache.put(userById, CacheHolder(messageList, ++messageInt))
                user.sendPrivateMessage(
                    authChannel, questions[messageInt]
                )
                return
            }
            3 -> {
                messageList.add(message)
                val verificationChannel =
                    guild.getTextChannelById(authorizationConfig.verificationID).let {
                        if (it == null) {
                            logger.error("Verification channel ID is invalid!")
                            return
                        }
                        it
                    }
                verificationChannel.sendMessage(messageList.toString()).queue()
                messageCache.invalidate(userById)
            }
        }
    }


    private fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val user = event.user
        val emote = event.guild.retrieveEmoteById(authorizationConfig.emoteID).complete()
        val channel = event.guild.getTextChannelById(authorizationConfig.channelID).let {
            if (it != null) return@let it
            else {
                logger.error("Channel does not exist, or ChannelID is invalid")
                return
            }
        }

        channel.retrieveMessageById(authorizationConfig.messageID).submit().thenAccept {
            it.removeReaction(emote, user).queue()
        }
    }

    override fun register(jda: JDA) {
        jda.listenFlow<GuildMessageReactionAddEvent>().handleEachIn(scope, this::onGuildEmoteReact)
        jda.listenFlow<MessageReceivedEvent>().handleEachIn(scope, this::onPrivateMessageReceived)
        jda.listenFlow<GuildMemberRemoveEvent>().handleEachIn(scope, this::onGuildMemberRemove)
    }
}