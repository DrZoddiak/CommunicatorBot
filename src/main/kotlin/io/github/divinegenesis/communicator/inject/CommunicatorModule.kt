package io.github.divinegenesis.communicator.inject

import com.google.inject.Singleton
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.github.divinegenesis.communicator.config.ConfigManager
import net.dv8tion.jda.api.JDA

class CommunicatorModule(private val configManager: ConfigManager) : KotlinModule() {
    override fun configure() {
        bind<JDA>().toProvider<JDAProvider>().`in`<Singleton>()
        bind<EventWaiter>().asEagerSingleton()
        bind<ConfigManager>().toInstance(configManager)
    }
}