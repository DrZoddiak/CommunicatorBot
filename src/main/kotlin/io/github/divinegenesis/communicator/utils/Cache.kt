package io.github.divinegenesis.communicator.utils

data class CacheHolder(
    val messageList: MutableList<String> = mutableListOf(),
    val messageInt: Int = 0
)