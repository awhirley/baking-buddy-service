package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.api.errors.requireUuidParam
import com.bakingbuddy.services.RecipeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlin.uuid.Uuid

fun Route.recipeRoutes(
    recipeService: RecipeService
) {
    get(path = "/api/recipes/{id}") {
        val id = call.parameters["id"]
          ?: throw BadRequestException("Path parameter 'id' must be provided")

        val uuid = call.requireUuidParam("id")
        val recipe = recipeService.getRecipe(uuid)
          ?: throw NotFoundException("Recipe", id)

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
    
    patch(path = "/api/recipes/{id}") {
        val id = call.parameters["id"]
          ?: throw BadRequestException("Path parameter 'id' must be provided")
          
        val uuid = call.requireUuidParam("id")
        val recipe = recipeService.editRecipe(uuid, call.receive())
        
        call.respond(recipe)
    }

    patch(path = "/api/ingredients/{id}") {
        val id = call.parameters["id"]
          ?: throw BadRequestException("Path parameter 'id' must be provided")

        val uuid = call.requireUuidParam("id")
        val ingredient = recipeService.editIngredient(uuid, call.receive())
        call.respond(ingredient)
    }

    patch(path = "/api/instructions/{id}") {
        val id = call.parameters["id"]
          ?: throw BadRequestException("Path parameter 'id' must be provided")

        val uuid = call.requireUuidParam("id")
        val instruction = recipeService.editInstruction(uuid, call.receive())
        call.respond(instruction)
    }

    patch(path = "/api/recipes/notes/{id}") {
        val id = call.parameters["id"]
          ?: throw BadRequestException("Path parameter 'id' must be provided")

        val uuid = call.requireUuidParam("id")
        val recipe = recipeService.updateRecipeNotes(uuid, call.receive())
        call.respond(recipe)
    }

    patch(path = "/api/ingredients/notes/{id}") {
        val id = call.parameters["id"]
          ?: throw BadRequestException("Path parameter 'id' must be provided")

        val uuid = call.requireUuidParam("id")
        val ingredient = recipeService.updateIngredientNotes(uuid, call.receive())
        call.respond(ingredient)
    }

    patch(path = "/api/instructions/notes/{id}") {
        val id = call.parameters["id"]
          ?: throw BadRequestException("Path parameter 'id' must be provided")

        val uuid = call.requireUuidParam("id")
        val instruction = recipeService.updateInstructionNotes(uuid, call.receive())
        call.respond(instruction)
    }

    delete(path = "/api/recipes/{id}") {

        val id = call.parameters["id"]
          ?: throw BadRequestException("Path parameter 'id' must be provided")

        val uuid = call.requireUuidParam("id")
        
        recipeService.deleteRecipe(uuid)
        call.respond(HttpStatusCode.NoContent)
    }
}