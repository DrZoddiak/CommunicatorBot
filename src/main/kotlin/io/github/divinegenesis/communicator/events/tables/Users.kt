package io.github.divinegenesis.communicator.events.tables

import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object Users : IdTable<Long>() {
    override val id = long("uid").entityId()
    val username = varchar("username", 37) //Username & Discriminator
    val reacted = bool("reacted").default(false)
    val processed = bool("processed").default(false)
    val processing = bool("processing").default(false)
    val roles = text("roles").default("")
    val leaves = integer("times left").default(0)
    override val primaryKey = PrimaryKey(id)
}

class UserStore(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserStore>(Users)

    var username by Users.username

    var reacted by Users.reacted
        private set
    var processed by Users.processed
        private set
    var roles by Users.roles
        private set
    var leaves by Users.leaves
        private set
    var processing by Users.processing
        private set

    suspend fun setProcessed(value: Boolean) = newSuspendedTransaction {
        processed = value
    }

    suspend fun setReacted(value: Boolean) = newSuspendedTransaction {
        reacted = value
    }

    suspend fun setRoles(value: String) = newSuspendedTransaction {
        roles = value
    }

    suspend fun setProcessing(value: Boolean) = newSuspendedTransaction {
        processing = value
    }

    suspend fun addLeaves() = newSuspendedTransaction {
        leaves += 1
    }

    override fun equals(other: Any?): Boolean {
        return other is UserStore && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

object UserTransaction {
    suspend fun getOrCreate(user: User) = newSuspendedTransaction {
        val userId = user.idLong
        UserStore.findById(userId) ?: UserStore.new(userId) {
            username = "${user.name}#${user.discriminator}"
        }
    }

    suspend fun all() = newSuspendedTransaction {
        UserStore.all().toList()
    }
}