package com.bakingbuddy.routes

import com.bakingbuddy.services.RecipeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlin.uuid.Uuid

fun Route.recipeRoutes(
    recipeService: RecipeService
) {
    get(path = "/api/recipes/{id}") {

        val id = call.parameters["id"]

        if (id == null) {
            call.respondText(
                "Invalid recipe ID",
                status = io.ktor.http.HttpStatusCode.BadRequest
            )
            return@get
        }

        val recipe = recipeService.getRecipe(Uuid.parse(id))

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
    
    post(path = "/api/recipes") {
        val recipe = recipeService.createRecipe(call.receive())
        call.respond(HttpStatusCode.Created, recipe)
    }
}