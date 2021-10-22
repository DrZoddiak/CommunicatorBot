package io.github.divinegenesis.communicator.utils

import club.minnced.jda.reactor.on
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.requests.RestAction

val scope = CoroutineScope(Dispatchers.Default)

inline fun <reified T : GenericEvent> JDA.listenFlow() = on<T>().asFlow()

inline fun <T> Flow<T>.handleEachIn(scope: CoroutineScope, crossinline run: suspend (T) -> Unit) = scope.launch {
    collect {
        launch {
            try {
                run(it)
            } catch (e: Exception) {
                e.message ?: e.printStackTrace()
            }
        }
    }
}

suspend fun <T> RestAction<T>.await(): T = submit().await()