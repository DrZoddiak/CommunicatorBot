package io.github.divinegenesis.communicator.utils

import com.github.benmanes.caffeine.cache.Caffeine

data class CacheHolder(
    val messageList: MutableList<String> = mutableListOf(),
    val messageInt: Int = 0
)