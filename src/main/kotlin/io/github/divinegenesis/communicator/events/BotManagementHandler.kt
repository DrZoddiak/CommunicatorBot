package io.github.divinegenesis.communicator.events

import com.google.inject.Inject
import io.github.divinegenesis.communicator.config.ConfigManager
import io.github.divinegenesis.communicator.utils.handleEachIn
import io.github.divinegenesis.communicator.utils.listenFlow
import io.github.divinegenesis.communicator.utils.scope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent

class BotManagementHandler @Inject constructor(configManager: ConfigManager) : EventListener {
    private val authorizedBots = configManager.config.authorizedBotList

    //Specifically for tracking bot joins
    private fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val user = event.user
        if (!user.isBot) return
        if (authorizedBots.contains(user.id)) return

        event.member.ban(1, "Unauthorized bot!").queue()
    }

    private fun onGuildMemberPunished(event: GuildMemberRemoveEvent) {

    }

    override fun register(jda: JDA) {
        jda.listenFlow<GuildMemberJoinEvent>().handleEachIn(scope, this::onGuildMemberJoin)
    }
}