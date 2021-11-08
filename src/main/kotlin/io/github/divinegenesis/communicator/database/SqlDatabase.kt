package io.github.divinegenesis.communicator.database

import com.google.inject.Inject
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.divinegenesis.communicator.config.ConfigManager
import io.github.divinegenesis.communicator.events.tables.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class SqlDatabase @Inject constructor(configManager: ConfigManager) {
    private val databaseConfig = configManager.config.databaseConfig
    suspend fun loadDatabase() {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${databaseConfig.host}:${databaseConfig.port}/${databaseConfig.dbName}"
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username = databaseConfig.dbUsername
            password = databaseConfig.dbPassword
        }
        Database.connect(HikariDataSource(hikariConfig))
        newSuspendedTransaction {
            SchemaUtils.create(Users)
        }
    }
}