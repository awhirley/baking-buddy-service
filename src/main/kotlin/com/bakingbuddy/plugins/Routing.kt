package com.bakingbuddy.plugins

import com.bakingbuddy.api.routes.bakeRoutes
import com.bakingbuddy.api.routes.deltaRoutes
import com.bakingbuddy.api.routes.healthRoutes
import com.bakingbuddy.api.routes.recipeRoutes
import com.bakingbuddy.services.BakeService
import com.bakingbuddy.services.DeltaService
import com.bakingbuddy.services.RecipeService
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    
    val recipeService = RecipeService()
    val deltaService = DeltaService()
    val bakeService = BakeService()

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
        bakeRoutes(bakeService)
    }
}