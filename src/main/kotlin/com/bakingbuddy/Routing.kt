package com.bakingbuddy

import com.bakingbuddy.routes.healthRoutes
import com.bakingbuddy.routes.recipeRoutes
import com.bakingbuddy.services.RecipeService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    
    val recipeService = RecipeService()

    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }

        healthRoutes()
        recipeRoutes(recipeService)
    }
}