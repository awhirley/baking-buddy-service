package com.bakingbuddy.routes

import com.bakingbuddy.services.RecipeService
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.recipeRoutes(
    recipeService: RecipeService
) {
    get(path = "/api/recipes/{id}") {

        val id = call.parameters["id"]?.let { UUID.fromString(it) }

        if (id == null) {
            call.respondText(
                "Invalid recipe ID",
                status = io.ktor.http.HttpStatusCode.BadRequest
            )
            return@get
        }

        val recipe = recipeService.getRecipe(id)

        if (recipe == null) {
            call.respond(
                io.ktor.http.HttpStatusCode.NotFound
            )
            return@get
        }

        call.respond(recipe)
    }
    
    get(path = "/api/recipes") {

        val recipes = recipeService.listRecipes()
        call.respond(recipes)
    }
}