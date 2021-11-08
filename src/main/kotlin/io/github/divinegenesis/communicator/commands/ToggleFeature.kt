package io.github.divinegenesis.communicator.commands

import com.google.inject.Inject
import io.github.divinegenesis.communicator.config.ConfigManager
import me.mattstudios.mfjda.annotations.Command
import me.mattstudios.mfjda.annotations.Default
import me.mattstudios.mfjda.annotations.Requirement
import me.mattstudios.mfjda.base.CommandBase

@Command("toggle")
class ToggleFeature @Inject constructor(private val configManager: ConfigManager) : CommandBase() {


    @Requirement("owner")
    @Default
    fun defaultCommand() {
        configManager.config.features.removeReaction = true
        configManager.loadConfig()
    }
}