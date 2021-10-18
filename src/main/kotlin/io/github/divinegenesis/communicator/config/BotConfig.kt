package io.github.divinegenesis.communicator.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
class BotConfig {

    val mainConfiguration = MainConfiguration()

    val databaseConfig = DatabaseConfig()

    val authorizationConfig = AuthorizationConfig()

    @Setting("Authorized-Bots")
    @Comment("This should be the bots UserID, for example Dyno's UserID is \"155149108183695360\"")
    val authorizedBotList = listOf("")
}

@ConfigSerializable
class MainConfiguration {

    @Setting("GuildID")
    val guildID = ""

    @Setting("OwnerID")
    val ownerID = ""

    val debugChannel = ""

    val botToken = ""

}

@ConfigSerializable
class AuthorizationConfig {

    @Setting("EmoteID")
    val emoteID = ""

    @Setting("ChannelID")
    @Comment("The channel ID of where to listen for the emote")
    val channelID = ""

    @Setting("Verification-ChannelID")
    @Comment("The channel id where verification is sent to")
    val verificationID = ""

    @Setting("MessageID")
    @Comment("The message ID, used for removing a users emote if they leave")
    val messageID = ""

}

@ConfigSerializable
class DatabaseConfig {

    @Setting("Database-Host")
    val host = "localhost"

    @Setting("Database-Port")
    val port = "3306"

    @Setting("Database-Name")
    val dbName = ""

    @Setting("Username")
    val dbUsername = ""

    @Setting("Password")
    val dbPassword = ""
}