package io.github.divinegenesis.communicator.commands

import com.google.inject.Inject
import io.github.divinegenesis.communicator.config.ConfigManager
import io.github.divinegenesis.communicator.utils.parseReactions
import me.mattstudios.mfjda.annotations.Command
import me.mattstudios.mfjda.annotations.Default
import me.mattstudios.mfjda.annotations.Requirement
import me.mattstudios.mfjda.base.CommandBase

@Command("initiate")
class InitiateCommand @Inject constructor(private val configManager: ConfigManager) : CommandBase() {
    val config = configManager.config.authorizationConfig

    @Requirement("#owner")
    @Default
    suspend fun defaultCommand() {
        val channel = message.guild.getTextChannelById(configManager.config.authorizationConfig.authorizationChannelID)

        parseReactions(channel, config.questions.prepStatement, config)
    }
}