package io.github.divinegenesis.communicator.utils

import com.github.benmanes.caffeine.cache.Caffeine

data class CacheHolder(
    val messageList: MutableList<String>,
    val messageInt: Int
) {}