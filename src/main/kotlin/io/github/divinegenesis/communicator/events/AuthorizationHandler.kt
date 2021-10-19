package io.github.divinegenesis.communicator.events

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.inject.Inject
import io.github.divinegenesis.communicator.config.ConfigManager
import io.github.divinegenesis.communicator.logging.logger
import io.github.divinegenesis.communicator.utils.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent


class AuthorizationHandler @Inject constructor(configManager: ConfigManager) : EventListener {

    private val config = configManager.config
    private val mainConfiguration = config.mainConfiguration
    private val authorizationConfig = config.authorizationConfig
    private val logger = logger<AuthorizationHandler>()

    private val messageCache = Caffeine.newBuilder()
        .build<String, CacheHolder>()

    private fun onGuildEmoteReact(event: GuildMessageReactionAddEvent) {
        if (event.user.isBot) return
        logger.debug("Emote reacted")
        val emote = event.reactionEmote.let {
            if (it.isEmote) return@let it.id
            return
        }
        val channel = event.channel

        logger.info("comparing emote ID")
        if (emote != authorizationConfig.emoteID) return
        logger.info("Channel: ${channel.id} - Config: ${authorizationConfig.channelID}")
        if (channel.id != authorizationConfig.channelID) return

        val user = event.user

        logger.info(user.id)

        user.sendPrivateMessage(
            channel, """
            *Greetings, my dear Anonimian.*
            *You will be authorized after you answer three questions, __understood?__*
        """.trimIndent()
        )
    }

    private fun onGuildVerifyEmoteReact(event: GuildMessageReactionAddEvent) {
        if (event.user.isBot) return
        if (event.channel.id != authorizationConfig.verificationID) return

        val message = event.retrieveMessage().submit().get()
        val mentionedMembers = message.mentionedMembers

        val guild = event.guild
        //There should only be one Member in the list ever
        val member = if (mentionedMembers.isNotEmpty()) mentionedMembers[0] else return
        val regularRole = guild.getRoleById(authorizationConfig.regularRoleID).let {
            if (it == null) {
                logger.error("Regular Role ID is invalid!")
                return
            }
            it
        }
        val specialRole = guild.getRoleById(authorizationConfig.specialRoleID).let {
            if (it == null) {
                logger.error("Special Role ID is invalid!")
                return
            }
            it
        }

        val verdict = event.reactionEmote.emote.id

        if (verdict == authorizationConfig.approveEmote) {
            guild.addRoleToMember(member, regularRole).queue()
            if (message.contentRaw.contains(authorizationConfig.password, true)) {
                guild.addRoleToMember(member, specialRole).queue()
            }

        } else {
            //todo: Awaiting what to do when user is denied
        }
    }

    private fun onPrivateMessageReceived(event: MessageReceivedEvent) {
        if (event.channelType != ChannelType.PRIVATE) return
        if (event.author.isBot) return

        val guild = event.jda.getGuildById(mainConfiguration.guildID).let {
            if (it == null) {
                logger.error("Guild ID is invalid!")
                return
            }
            it
        }
        val authChannel = guild.getTextChannelById(authorizationConfig.channelID).let {
            if (it == null) {
                logger.error("Authentication channel ID is invalid!")
                return
            }
            it
        }

        val message = event.message.contentRaw
        val user = event.author
        val userById = user.id

        val cache =
            messageCache.getIfPresent(userById).let {
                if (it != null) {
                    it
                } else {
                    messageCache.put(userById, CacheHolder(mutableListOf("$message\n")))
                    messageCache.getIfPresent(userById)!!
                }
            }

        val messageList = cache.messageList
        var messageInt = cache.messageInt
        val questions = authorizationConfig.questions


        when (messageInt) {
            0, 1, 2 -> {
                if (messageInt != 0) {
                    messageList.add("$message\n")
                }
                user.sendPrivateMessage(
                    authChannel, questions[messageInt]
                )
                messageCache.put(userById, CacheHolder(messageList, ++messageInt))
                return
            }
            3 -> {
                messageList.add(message)
                guild.getTextChannelById(authorizationConfig.verificationID).let {
                    it?.sendMessage(
                        """
                        User: ${user.asMention}
                        
                        $messageList
                    """.trimIndent()
                    )?.queue()
                }
                messageCache.invalidate(userById)
            }
        }
    }

    private fun onVerificationChannelMessageReceived(event: GuildMessageReceivedEvent) {
        if (!event.author.isBot) return

        val message = event.message

        val approved = event.guild.getEmoteById(authorizationConfig.approveEmote)!!
        val denied = event.guild.getEmoteById(authorizationConfig.denyEmote)!!

        message.addReaction(approved).and(message.addReaction(denied)).queue()
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
        jda.listenFlow<GuildMessageReceivedEvent>().handleEachIn(scope, this::onVerificationChannelMessageReceived)
        jda.listenFlow<GuildMessageReactionAddEvent>().handleEachIn(scope, this::onGuildVerifyEmoteReact)
    }
}