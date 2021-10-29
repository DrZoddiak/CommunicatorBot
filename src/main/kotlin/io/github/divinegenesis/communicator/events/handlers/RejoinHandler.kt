package io.github.divinegenesis.communicator.events.handlers

import com.google.inject.Inject
import io.github.divinegenesis.communicator.config.ConfigManager
import io.github.divinegenesis.communicator.events.EventListener
import io.github.divinegenesis.communicator.events.tables.JoinLeaves
import io.github.divinegenesis.communicator.utils.handleEachIn
import io.github.divinegenesis.communicator.utils.listenFlow
import io.github.divinegenesis.communicator.utils.scope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class RejoinHandler @Inject constructor(configManager: ConfigManager) : EventListener {
    private val config = configManager.config.authorizationConfig

    private suspend fun onGuildJoin(event: GuildMemberJoinEvent) {
        val user = event.user

        val list = newSuspendedTransaction {
            JoinLeaves.select {
                JoinLeaves.uid eq user.idLong
                JoinLeaves.type eq JoinLeaves.Type.LEAVE
            }.toList()
        }

        if (list.isEmpty()) return

        val guild = event.guild

        guild.getRoleById(config.rejoinRoleID)?.let {
            guild.addRoleToMember(user.idLong, it).queue()
        }
    }

    override fun register(jda: JDA) {
        jda.listenFlow<GuildMemberJoinEvent>().handleEachIn(scope, this::onGuildJoin)
    }
}