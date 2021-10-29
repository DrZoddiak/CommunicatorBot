package io.github.divinegenesis.communicator.commands

import me.mattstudios.mfjda.annotations.Command
import me.mattstudios.mfjda.annotations.Default
import me.mattstudios.mfjda.base.CommandBase

@Command("ping")
class PingCommand : CommandBase() {
    @Default
    fun defaultCommand() {
        message.textChannel.sendMessage("Pong!").queue()
    }
}