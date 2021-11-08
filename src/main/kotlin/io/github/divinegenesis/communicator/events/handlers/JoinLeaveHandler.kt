package io.github.divinegenesis.communicator.events.handlers

import com.google.inject.Inject
import io.github.divinegenesis.communicator.config.ConfigManager
import io.github.divinegenesis.communicator.events.EventListener
import io.github.divinegenesis.communicator.events.tables.UserTransaction
import io.github.divinegenesis.communicator.logging.logger
import io.github.divinegenesis.communicator.utils.handleEachIn
import io.github.divinegenesis.communicator.utils.leaves
import io.github.divinegenesis.communicator.utils.listenFlow
import io.github.divinegenesis.communicator.utils.scope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent

class JoinLeaveHandler @Inject constructor(configManager: ConfigManager) : EventListener {
    val config = configManager.config
    val logger = logger<JoinLeaveHandler>()

    private suspend fun onGuildJoin(event: GuildMemberJoinEvent) {
        val user = event.user
        if (user.isBot) return

        if (user.leaves() < 1) return
        val guild = event.guild

        logger.info("User joined that has left before, attempting to apply role")

        guild.getRoleById(config.authorizationConfig.rejoinRoleID)?.let {
            guild.addRoleToMember(event.member, it).queue()
        }
    }

    private suspend fun onGuildLeave(event: GuildMemberRemoveEvent) {
        val user = event.user
        if (user.isBot) return

        logger.info("${user.asTag} has left the guild, updating DB to reflect this.")

        UserTransaction.getOrCreate(user).addLeaves()
    }

    override fun register(jda: JDA) {
        jda.listenFlow<GuildMemberJoinEvent>().handleEachIn(scope, this::onGuildJoin)
        jda.listenFlow<GuildMemberRemoveEvent>().handleEachIn(scope, this::onGuildLeave)
    }
}