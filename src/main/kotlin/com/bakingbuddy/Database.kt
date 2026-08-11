package com.bakingbuddy

import io.github.cdimascio.dotenv.Dotenv
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabase() {
    val dotenv = Dotenv.load()
    
    // TODO: this does not fail gracefully
    val url = dotenv.get("DATABASE_URL")
        ?: error("DATABASE_URL is not set")

    val user = dotenv.get("DATABASE_USER")
        ?: error("DATABASE_USER is not set")

    val password = dotenv.get("DATABASE_PASSWORD")
        ?: error("DATABASE_PASSWORD is not set")
        
    environment.log.info("Connecting to database with URL: $url, user: $user")

    Database.connect(
        url = url,
        user = user,
        password = password,
        driver = "org.postgresql.Driver"
    )
}