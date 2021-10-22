package io.github.divinegenesis.communicator.events.tables

object UserRoles : EventTable() {
    val uid = long("user_id")
    val username = varchar("username", 37) //Username & Discriminator
    val roles = text("roles")
}