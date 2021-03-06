package io.github.divinegenesis.communicator.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class BotConfig(

    val mainConfiguration: MainConfiguration = MainConfiguration(),

    val databaseConfig: DatabaseConfig = DatabaseConfig(),

    val authorizationConfig: AuthorizationConfig = AuthorizationConfig(),

    @Setting("Authorized-Bots")
    @Comment("This should be the bots UserID, for example Dyno's UserID is \"155149108183695360\"")
    val authorizedBotList: List<String> = listOf(""),

    val inviteMap: Map<String, String> = mapOf(Pair("", "")),

    val features: Features = Features(),
)

@ConfigSerializable
data class MainConfiguration(

    @Setting("GuildID")
    val guildID: String = "",

    val botToken: String = "",

    @Setting("Punishment-Role-ID")
    val punishmentRoleID: String = "",

    val prefix: String = "!"
)

@ConfigSerializable
data class AuthorizationConfig(

    @Setting("Emote-Verification-ID")
    @Comment("The ID of the emote users will click to be verified")
    val emoteID: String = "",

    @Setting("Emote-DM-ID")
    val emojiID: String = "",

    @Setting("Channel-Authorization-ID")
    @Comment("The channel ID of where to listen for the emote")
    val authorizationChannelID: String = "",

    @Setting("Channel-Authorize-ID")
    @Comment("The channel id where verification is sent to")
    val authorizationInspectionChannelID: String = "",

    @Setting("Message-ID")
    @Comment("The message ID, used for removing a users emote if they leave")
    val messageID: String = "",

    val questions: AuthorizationQuestions = AuthorizationQuestions(),

    val password: String = "",

    @Setting("Role-Special-ID")
    @Comment("The ID of the special role a user receives from getting the password correct")
    val specialRoleID: String = "",

    @Setting("Role-Regular-ID")
    @Comment("The ID of the regular role a user receives when approved")
    val regularRoleID: String = "",

    @Setting("Emote-Approval-ID")
    val approveEmote: String = "",

    @Setting("Emote-Deny-Id")
    val denyEmote: String = "",

    @Setting("Role-Rejoin-ID")
    val rejoinRoleID: String = ""
)

@ConfigSerializable
data class AuthorizationQuestions(

    val initialStatement: String = """
        **Greetings, my dear *status*. Whenever you are ready for the next step, apply your reaction below.**
        *You will be authorized after you answer six questions.*
        """.trimIndent(),

    val prepStatement: String = """
                    Greetings, my dear user of the Anonimian status within Universium.
            We've finally completed the authorization process, for you to be able to enter the server.
            In case you do not remember the server, take a look: https://discord.gg/EuCeC8V

            If you wish to finally be authorized, click the check mark below.
                """.trimIndent(),

    val questions: List<String> = listOf(

        """
        **Greetings, my dear *status*. Whenever you are ready for the next step, apply your reaction below.**
        *You will be authorized after you answer six questions.*
        """.trimIndent(),
        """
        *First of all: Thank you for being so kind to take the time for us, your dedication will not go unnoticed!*
        **1. Can you try to explain our Rule and Etiquette in your own words?**
        *The Rule stands for having to use your common sense at best of your ability & the Etiquette expects of you to do your best at being a good person.*
    """.trimIndent(),
        """
        **2. Have you checked and do you agree with the Fact?**
        *Agreeing to this fact means you understand and accept that other users may for example: create content by using the activity that is generated by you.*
    """.trimIndent(),
        "**3. What is the password?**"
    ),
)

@ConfigSerializable
data class DatabaseConfig(

    @Setting("Database-Host")
    val host: String = "localhost",

    @Setting("Database-Port")
    val port: String = "3306",

    @Setting("Database-Name")
    val dbName: String = "",

    @Setting("Username")
    val dbUsername: String = "",

    @Setting("Password")
    val dbPassword: String = "",
)

@ConfigSerializable
data class Features(

    @Comment("DO NOT TOUCH THIS SETTING")
    var firstRun: Boolean = false,

    var removeReaction: Boolean = false

)

/*
*   Banished users
* i apologize directly to universium for my wrong doings
* Print time remaining
 */