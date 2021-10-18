package io.github.divinegenesis.communicator.events.tables

object JoinLeaves : EventTable() {
    val uid = long("user_id")
    val username = varchar("username", 37) //Username & Discriminator
    val type = enumerationByName("type", 5, Type::class)

    enum class Type {
        JOIN,
        LEAVE
    }
}