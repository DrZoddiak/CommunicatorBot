package io.github.divinegenesis.communicator

import com.google.inject.Guice
import com.google.inject.Inject
import dev.misfitlabs.kotlinguice4.getInstance
import io.github.divinegenesis.communicator.commands.PingCommand
import io.github.divinegenesis.communicator.config.ConfigManager
import io.github.divinegenesis.communicator.database.SqlDatabase
import io.github.divinegenesis.communicator.events.ListenersModule
import io.github.divinegenesis.communicator.inject.CommunicatorModule
import io.github.divinegenesis.communicator.logging.logger
import kotlinx.coroutines.runBlocking
import me.mattstudios.mfjda.base.CommandManager
import net.dv8tion.jda.api.JDA
import kotlin.system.measureTimeMillis

class Communicator @Inject constructor() {
    private val logger = logger<Communicator>()
    private val configManager = ConfigManager().also {
        it.loadConfig()
    }

    fun load() {
        val injector = Guice.createInjector(CommunicatorModule(configManager), ListenersModule())
        val jda = injector.getInstance<JDA>()
        val commandManager = CommandManager(jda, configManager.config.mainConfiguration.prefix)
        commandManager.registerMessage("cmd.no.exists") {} //Don't send message if command isn't found

        val commands = listOf(
            PingCommand(),
        )

        commandManager.register(commands)

        val loadTime = measureTimeMillis {
            runBlocking {
                SqlDatabase(configManager).loadDatabase()
            }
        }
        logger.info("Loading database took $loadTime ms")

        jda.awaitReady()
    }
}







