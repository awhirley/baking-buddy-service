package com.bakingbuddy.plugins

import com.bakingbuddy.routes.deltaRoutes
import com.bakingbuddy.routes.healthRoutes
import com.bakingbuddy.routes.recipeRoutes
import com.bakingbuddy.services.DeltaService
import com.bakingbuddy.services.RecipeService
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    
    val recipeService = RecipeService()
    val deltaService = DeltaService()

    install(CORS) {
        allowHost("localhost:5173")
        allowHost("127.0.0.1:5173")

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)

        allowHeader(io.ktor.http.HttpHeaders.ContentType)
    }

    routing {
        healthRoutes()
        recipeRoutes(recipeService)
        deltaRoutes(deltaService)
    }
}