package com.bakingbuddy

import io.github.cdimascio.dotenv.Dotenv
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabase() {
    val dotenv = Dotenv.load()
    val environment: String? = System.getenv("APP_ENV") ?: dotenv.get("APP_ENV")
    
    if (environment == null) {
        error("APP_ENV is not set.")
    }
    
    val (url, user, password) = when (environment) {
        "production" -> Triple(
            System.getenv("DATABASE_URL"),
            System.getenv("DATABASE_USER"),
            System.getenv("DATABASE_PASSWORD")
        )
        "development" -> Triple(
            dotenv.get("DATABASE_URL"),
            dotenv.get("DATABASE_USER"),
            dotenv.get("DATABASE_PASSWORD")
        )
        else -> error("Unknown APP_ENV: $environment")
    }

    if (url == null) { error("DATABASE_URL is not set") }
    if (user == null) { error("DATABASE_USER is not set") }
    if (password == null) { error("DATABASE_PASSWORD is not set") }

    Database.connect(
        url = url,
        user = user,
        password = password,
        driver = "org.postgresql.Driver"
    )
}