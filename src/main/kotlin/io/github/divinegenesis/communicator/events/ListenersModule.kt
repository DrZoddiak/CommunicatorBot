package io.github.divinegenesis.communicator.events

import com.google.inject.Inject
import io.github.divinegenesis.communicator.inject.Classpath
import dev.misfitlabs.kotlinguice4.KotlinModule
import dev.misfitlabs.kotlinguice4.multibindings.KotlinMultibinder
import dev.misfitlabs.kotlinguice4.multibindings.KotlinMultibindingsScanner

class ListenersModule @Inject constructor() : KotlinModule() {
    override fun configure() {
        val listeners = Classpath.subtypesOf(EventListener::class.java)
        val binder = KotlinMultibinder.newSetBinder<EventListener>(kotlinBinder)

        for (listener in listeners) {
            binder.addBinding().to(listener)
        }

        install(KotlinMultibindingsScanner.asModule())

        super.configure()
    }
}