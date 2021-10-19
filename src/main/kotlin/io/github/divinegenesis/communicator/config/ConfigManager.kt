package io.github.divinegenesis.communicator.config

import com.google.inject.Inject
import io.github.divinegenesis.communicator.logging.logger
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.kotlin.extensions.get
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class ConfigManager @Inject constructor() {
    private val logger = logger<ConfigManager>()

    private val configName = "Communicator.conf"

    private lateinit var root: CommentedConfigurationNode
    lateinit var config: BotConfig

    private var path: Path = FileSystems.getDefault().getPath("config")
        get() {
            return if (!field.exists()) {
                field.createDirectories()
            } else {
                field
            }
        }

    private val configFile = File(path.toFile(), configName).also {
        if (!it.exists()) {
            it.createNewFile()
        }
    }

    private val loader = HoconConfigurationLoader.builder()
        .path(configFile.toPath())
        .build()

    fun loadConfig() {
        root = try {
            logger.info("Loading communicator.config..")
            loader.load()
        } catch (e: ConfigurateException) {
            logger.error("Unable to load your configuration! Sorry!")
            e.message ?: e.printStackTrace()
            return
        }
        config = root.get(BotConfig::class).let {
            if (it == null) {
                return
            }
            return@let it
        }
        try {
            loader.save(root)
        } catch (e: ConfigurateException) {
            logger.error("Unable to save your configuration! Sorry!")
            e.printStackTrace()
            return
        }
    }
}