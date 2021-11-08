package io.github.divinegenesis.communicator.events.handlers

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.inject.Inject
import io.github.divinegenesis.communicator.config.ConfigManager
import io.github.divinegenesis.communicator.events.EventListener
import io.github.divinegenesis.communicator.events.tables.UserTransaction
import io.github.divinegenesis.communicator.logging.logger
import io.github.divinegenesis.communicator.utils.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.concurrent.schedule

class AuthorizationHandler @Inject constructor(configManager: ConfigManager) : EventListener {

    private val config = configManager.config
    private val mainConfiguration = config.mainConfiguration
    private val authorizationConfig = config.authorizationConfig
    private val logger = logger<AuthorizationHandler>()

    private val processingCache = Caffeine.newBuilder()
        .build<Long, Boolean>()

    private val messageCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(6))
        .build<String, CacheHolder>()

    //Todo: If reaction is removed, user will need to re-apply
    //Remove ticket after 1 minute if mark isn't re-applied

    private suspend fun onGuildEmoteReact(event: GuildMessageReactionAddEvent) {
        if (event.user.isBot) return

        val channel = event.channel

        if (channel.id != authorizationConfig.authorizationChannelID) return

        val emote = event.reactionEmote.let {
            if (it.isEmote) return@let it.id
            return
        }

        if (emote != authorizationConfig.emoteID) return

        val user = event.user

        UserTransaction.getOrCreate(user).setReacted(true)
        if (user.wasProcessed()) return

        user.sendPrivateMessage(
            channel, authorizationConfig.questions.initialStatement
        )
    }

    private suspend fun onPrivateEmoteReact(event: PrivateMessageReactionAddEvent) {
        val user = event.retrieveUser().await()
        if (user.isBot) return

        val emojiId = event.reactionEmote.emoji

        if (authorizationConfig.emojiID == emojiId)
            user.sendPrivateMessage(
                null, authorizationConfig.questions.initialStatement
            )

    }

    private suspend fun onGuildVerifyEmoteReact(event: GuildMessageReactionAddEvent) {
        if (event.user.isBot) return
        if (event.channel.id != authorizationConfig.authorizationInspectionChannelID) return

        val message = event.retrieveMessage().await()
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
                //Todo: Hardcoded BAD
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

        val user = member?.user

        if (verdict == authorizationConfig.approveEmote) {

            //todo: Check if reacted exists
            //Edit message to reflect if user removed reaction
            if (user?.reacted() == false) {
                message.editMessage("User removed reaction from the message")
                Timer().schedule(
                    delay = 60000L
                ) {
                    message.delete()
                    user.sendPrivateMessage(
                        null, """
                            You have
                        """.trimIndent()
                    )
                }
            }

            member?.let { guild.addRoleToMember(it, regularRole).queue() }
            if (userPasswordAttempt.equals(authorizationConfig.password, true)) {
                member?.let { guild.addRoleToMember(it, specialRole).queue() }
                user?.sendPrivateMessage(
                    null, """
                        **Please note that a penalty will be administired tied to your Discord account if you leave Universium.**
                        ||*This means that if you leave and then come back someday, we won't be this cool to you!*||
                        *We hope that you will enjoy yourself and be able to find the friends you're looking for!!*
                        *You've been authorized as a Mysterian.*
                    """.trimIndent()
                )
            }
        } else {
            user?.sendPrivateMessage(
                null, """
                    Unfortunately your authorization request has been denied. You may try again in 10 minutes. Please take the time to think about your answers while you wait, for your next attempt.
                """.trimIndent()
            )
            Timer().schedule(
                delay = 600000L
            ) {
                user?.sendPrivateMessage(
                    null, authorizationConfig.questions.initialStatement
                )
            }
        }
    }

    private suspend fun onPrivateMessageReceived(event: MessageReceivedEvent) {
        if (event.channelType != ChannelType.PRIVATE) return
        if (event.author.isBot) return

        val guild = event.jda.getGuildById(mainConfiguration.guildID)
        val message = event.message.contentRaw
        val user = event.author

        if (user.wasProcessed()) return

        if (user.isProcessing()) return

        if (!user.reacted()) {
            user.sendPrivateMessage(
                null,
                "If you wish to be authorized please react to the message in <#${authorizationConfig.authorizationChannelID}>"
            )
            return
        }

        processingCache.put(user.idLong, true)

        processUser(user, message, guild)
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
        val channel = event.guild.getTextChannelById(authorizationConfig.authorizationChannelID)

        channel.let {
            it?.retrieveMessageById(authorizationConfig.messageID)?.await()?.removeReaction(emote, user)
        }
    }

    private suspend fun onBotReady(event: ReadyEvent) {
        parseReactions(
            event.jda.getTextChannelById(authorizationConfig.authorizationChannelID),
            authorizationConfig.questions.initialStatement,
            authorizationConfig
        )
    }

    private suspend fun processUser(user: User, message: String, guild: Guild?) {
        logger.info("Processing user")
        val userById = user.id
        val authChannel = guild?.getTextChannelById(authorizationConfig.authorizationChannelID)

        if (user.isProcessing()) return

        val cache = messageCache.getIfPresent(userById).let {
            if (it != null) {
                logger.info("was not null")
                it
            } else {
                logger.info("New cache")
                messageCache.put(userById, CacheHolder(mutableListOf(message)))
                messageCache.getIfPresent(userById)!!
            }
        }


        val messageInt = cache.messageInt
        val messageList = cache.messageList
        val questions = authorizationConfig.questions.questions
        val maxQuestions = questions.size

        if (messageInt != maxQuestions) {
            val question = questions[messageInt]
            if (messageInt != 0) {
                messageList.add("$message\n")
            }
            user.sendPrivateMessage(
                authChannel, question
            )
            messageCache.put(userById, CacheHolder(messageList, messageInt + 1))
            return
        } else {
            logger.info("Reached")
            val embed = EmbedBuilder()
                .setTitle(user.asTag)
                .setTimestamp(Instant.now())
                .setFooter(userById)

            messageList.add(message)

            for ((i, answer) in messageList.withIndex()) {
                if (i == 0) continue
                if (i == messageList.size) {
                    embed.addField(MessageEmbed.Field("Password", answer, false))
                }
                embed.addField(MessageEmbed.Field("Answer $i", answer, false))
            }

            user.sendPrivateMessage(
                null, """
                    Thank you very much for your authorization request. Your ticket will be reviewed shortly. Please be patient as this may take some time.
                """.trimIndent()
            )

            guild?.getTextChannelById(authorizationConfig.authorizationInspectionChannelID).let {
                it?.sendMessageEmbeds(embed.build())?.queue()
            }
            UserTransaction.getOrCreate(user).setProcessing(true)
            messageCache.invalidate(userById)
        }
    }

    override fun register(jda: JDA) {
        jda.listenFlow<ReadyEvent>().handleEachIn(scope, this::onBotReady)
        jda.listenFlow<MessageReceivedEvent>().handleEachIn(scope, this::onPrivateMessageReceived)
        jda.listenFlow<GuildMemberRemoveEvent>().handleEachIn(scope, this::onGuildMemberRemove)
        jda.listenFlow<GuildMessageReceivedEvent>().handleEachIn(scope, this::onVerificationChannelMessageReceived)
        jda.listenFlow<GuildMessageReactionAddEvent>().handleEachIn(scope, this::onGuildEmoteReact)
        jda.listenFlow<GuildMessageReactionAddEvent>().handleEachIn(scope, this::onGuildVerifyEmoteReact)
        jda.listenFlow<PrivateMessageReactionAddEvent>().handleEachIn(scope, this::onPrivateEmoteReact)
    }
}