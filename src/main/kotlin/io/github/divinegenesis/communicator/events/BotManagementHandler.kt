package io.github.divinegenesis.communicator.events

import com.google.inject.Inject
import io.github.divinegenesis.communicator.config.ConfigManager
import io.github.divinegenesis.communicator.events.tables.UserRoles
import io.github.divinegenesis.communicator.utils.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.time.OffsetDateTime

class BotManagementHandler @Inject constructor(configManager: ConfigManager) : EventListener {
    private val mainConfig = configManager.config.mainConfiguration
    private val authorizedBots = configManager.config.authorizedBotList

    //Specifically for tracking bot joins
    private fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val user = event.user
        if (!user.isBot) return
        if (authorizedBots.contains(user.id)) return

        event.member.ban(1, "Unauthorized bot!").queue()
    }

    private suspend fun onGuildMemberPunished(event: GuildMemberRemoveEvent) {
        val removedUser = event.user
        val guild = event.guild
        val time = OffsetDateTime.now().minusSeconds(30) //Look back 30 seconds from time event fired.

        for (auditLogEntry in guild.retrieveAuditLogs().await()) {
            if (auditLogEntry.timeCreated.isBefore(time)) continue
            if (auditLogEntry.type != ActionType.KICK && auditLogEntry.type != ActionType.BAN) continue
            val offendingUser = auditLogEntry.user
            //If removed user is a bot, and the offender wasn't self.
            if (removedUser.isBot && guild.selfMember != offendingUser?.let { guild.getMember(it) }) {
                offendingUser?.let { user ->
                    guild.getMember(user)?.let { member ->
                        newSuspendedTransaction {
                            UserRoles.insert {
                                it[timestamp] = LocalDateTime.now()
                                it[uid] = user.idLong
                                it[username] = "${user.name}#${user.discriminator}"
                                it[roles] = member.roles.toString()
                            }
                        }
                        member.roles.forEach { role ->
                            guild.removeRoleFromMember(member, role).queue()
                        }
                        guild.getRoleById(mainConfig.punishmentRoleID)?.let { role ->
                            guild.addRoleToMember(member, role).queue()
                        }
                        guild.retrieveOwner().await().user.sendPrivateMessage(
                            null, """
                                ${member.effectiveName} has removed a bot from ${guild.name}!
                                """.trimIndent()
                        )

                    }
                }
            } else {
                offendingUser?.let { bot ->
                    guild.ban(bot, 0).queue()
                }
            }
        }
    }

    override fun register(jda: JDA) {
        jda.listenFlow<GuildMemberJoinEvent>().handleEachIn(scope, this::onGuildMemberJoin)
        jda.listenFlow<GuildMemberRemoveEvent>().handleEachIn(scope, this::onGuildMemberPunished)
    }
}