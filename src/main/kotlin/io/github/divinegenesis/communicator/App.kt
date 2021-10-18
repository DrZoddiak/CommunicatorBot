package io.github.divinegenesis.communicator

import io.github.divinegenesis.communicator.logging.logger

fun main() {
    val logger = logger<Communicator>()
    try {
        logger.info("Starting Communicator..")
        Communicator().load()
    } catch (e: Exception) {
        logger.info("Something has gone wrong! See error below.")
        if (e.message != null) {
            logger.info(e.message)
        }
        e.printStackTrace()
    }
}