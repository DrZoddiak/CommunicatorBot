package io.github.divinegenesis.communicator.events.handlers

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.inject.Inject
import io.github.divinegenesis.communicator.config.ConfigManager
import io.github.divinegenesis.communicator.events.EventListener
import io.github.divinegenesis.communicator.logging.logger
import io.github.divinegenesis.communicator.utils.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import java.time.Instant


class AuthorizationHandler @Inject constructor(configManager: ConfigManager) : EventListener {

    private val config = configManager.config
    private val mainConfiguration = config.mainConfiguration
    private val authorizationConfig = config.authorizationConfig
    private val logger = logger<AuthorizationHandler>()

    private val messageCache = Caffeine.newBuilder()
        .build<String, CacheHolder>()

    private fun onGuildEmoteReact(event: GuildMessageReactionAddEvent) {
        if (event.user.isBot) return

        val channel = event.channel

        if (channel.id != authorizationConfig.authorizationChannelID) return

        val emote = event.reactionEmote.let {
            if (it.isEmote) return@let it.id
            return
        }

        if (emote != authorizationConfig.emoteID) return

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
        if (event.channel.id != authorizationConfig.authorizationInspectionChannelID) return

        val message = event.retrieveMessage().submit().get()
        val guild = event.guild


        var userPasswordAttempt = ""
        var member: Member? = null

        message.embeds.forEach {
            if (!it.footer?.text.isNullOrEmpty()) {
                member = guild.getMemberById(it.footer!!.text!!)
            } else {
                return
            }
            it.fields.forEach { field ->
                if (field.name?.startsWith("Answer 3") == true) {
                    userPasswordAttempt = field.value.toString()
                }
            }
        }

        //There should only be one Member in the list ever
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
            member?.let { guild.addRoleToMember(it, regularRole).queue() }
            if (userPasswordAttempt.equals(authorizationConfig.password, true)) {
                member?.let { guild.addRoleToMember(it, specialRole).queue() }
            }
        } else {
            //todo: Awaiting what to do when user is denied
        }
    }

    private fun onPrivateMessageReceived(event: MessageReceivedEvent) {
        if (event.channelType != ChannelType.PRIVATE) return
        if (event.author.isBot) return

        val guild = event.jda.getGuildById(mainConfiguration.guildID)
        val authChannel = guild?.getTextChannelById(authorizationConfig.authorizationChannelID)

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

        var messageInt = cache.messageInt

        val messageList = cache.messageList
        val questions = authorizationConfig.questions
        val maxQuestions = questions.size
        val question = questions[messageInt]

        if (messageInt != maxQuestions) {
            if (messageInt != 0) {
                messageList.add("$message\n")
            }
            user.sendPrivateMessage(
                authChannel, question
            )
            messageCache.put(userById, CacheHolder(messageList, ++messageInt))
            return
        } else {
            val embed = EmbedBuilder()
                .setTitle(user.asTag)
                .setTimestamp(Instant.now())
                .setFooter(userById)

            messageList.add(message)

            for ((i, answer) in messageList.withIndex()) {
                embed.addField(MessageEmbed.Field("Answer $i", answer, false))
            }

            guild?.getTextChannelById(authorizationConfig.authorizationInspectionChannelID).let {
                it?.sendMessageEmbeds(embed.build())?.queue()
            }
            messageCache.invalidate(userById)
        }
    }

    private fun onVerificationChannelMessageReceived(event: GuildMessageReceivedEvent) {
        if (!event.author.isBot) return
        if (event.channel.id != authorizationConfig.authorizationInspectionChannelID) return

        val message = event.message

        val approved = event.guild.getEmoteById(authorizationConfig.approveEmote)!!
        val denied = event.guild.getEmoteById(authorizationConfig.denyEmote)!!

        message.addReaction(approved).and(message.addReaction(denied)).queue()
    }

    private suspend fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val user = event.user
        val emote = event.guild.retrieveEmoteById(authorizationConfig.emoteID).complete()
        val channel = event.guild.getTextChannelById(authorizationConfig.authorizationChannelID).let {
            if (it != null) return@let it
            else {
                logger.error("Channel does not exist, or ChannelID is invalid")
                return
            }
        }

        channel.retrieveMessageById(authorizationConfig.messageID).await().removeReaction(emote, user)
    }

    override fun register(jda: JDA) {
        jda.listenFlow<MessageReceivedEvent>().handleEachIn(scope, this::onPrivateMessageReceived)
        jda.listenFlow<GuildMemberRemoveEvent>().handleEachIn(scope, this::onGuildMemberRemove)
        jda.listenFlow<GuildMessageReceivedEvent>().handleEachIn(scope, this::onVerificationChannelMessageReceived)
        jda.listenFlow<GuildMessageReactionAddEvent>().handleEachIn(scope, this::onGuildEmoteReact)
        jda.listenFlow<GuildMessageReactionAddEvent>().handleEachIn(scope, this::onGuildVerifyEmoteReact)
    }
}