package io.github.divinegenesis.communicator.inject

import club.minnced.jda.reactor.ReactiveEventManager
import com.google.inject.Inject
import com.google.inject.Provider
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import io.github.divinegenesis.communicator.config.ConfigManager
import io.github.divinegenesis.communicator.events.EventListener
import io.github.divinegenesis.communicator.logging.logger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.util.*

class JDAProvider @Inject constructor(
    private val eventWaiter: EventWaiter,
    private val listeners: Set<EventListener>,
    private val config: ConfigManager
) : Provider<JDA> {
    override fun get(): JDA {
        val logger = logger<JDAProvider>()
        val manager = ReactiveEventManager()
        val jda = JDABuilder.createDefault(config.config.mainConfiguration.botToken)
            .enableIntents(EnumSet.allOf(GatewayIntent::class.java))
            .addEventListeners(eventWaiter)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .enableCache(CacheFlag.EMOTE)
            .setEventManager(manager)
            .build()

        listeners.forEach {
            logger.info("Registering Listener $it")
            it.register(jda)
        }
        return jda
    }
}