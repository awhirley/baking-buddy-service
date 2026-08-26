package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.api.errors.requireUuidParam
import com.bakingbuddy.api.errors.validate
import com.bakingbuddy.models.recipes.CreateRecipePayload
import com.bakingbuddy.models.recipes.EditRecipePayload
import com.bakingbuddy.services.RecipeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

@Suppress("ThrowsCount")
fun Route.recipeRoutes(recipeService: RecipeService) {
  get(path = "/api/recipes/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val uuid = call.requireUuidParam("id")
    val recipe =
      recipeService.getRecipe(uuid)
        ?: throw NotFoundException("Recipe", id)

    call.respond(recipe)
  }

  get(path = "/api/recipes") {
    val recipes = recipeService.listRecipes()
    call.respond(recipes)
  }

  post(path = "/api/recipes") {
    val payload = call.receive<CreateRecipePayload>()

    validate {
      requireNotBlank(payload.name, "name")
      requireNotBlankIfPresent(payload.description, "description")
      requireNotBlankIfPresent(payload.recipeSource, "recipeSource")
      requireNotBlankIfPresent(payload.recipeSourceType, "recipeSourceType")

      require(payload.ingredients.isNotEmpty(), "ingredients", "must contain at least one ingredient")
      require(payload.instructions.isNotEmpty(), "instructions", "must contain at least one instruction")

      payload.ingredients.forEachIndexed { index, ingredient ->
        requireNotBlank(ingredient.name, "ingredients[$index].name")
        requireNotBlank(ingredient.amount, "ingredients[$index].amount")
      }

      payload.instructions.forEachIndexed { index, instruction ->
        requireNotBlank(instruction, "instructions[$index]")
      }

      payload.tags?.forEachIndexed { index, tag ->
        requireNotBlank(tag, "tags[$index]")
      }

      payload.tools?.forEachIndexed { index, tool ->
        requireNotBlank(tool, "tools[$index]")
      }
    }

    val recipe = recipeService.createRecipe(payload)
    call.respond(HttpStatusCode.Created, recipe)
  }

  patch(path = "/api/recipes/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val payload = call.receive<EditRecipePayload>()

    validate {
      requireNotBlankIfPresent(payload.name, "name")
      requireNotBlankIfPresent(payload.description, "description")

      payload.tags?.forEachIndexed { index, tag ->
        requireNotBlank(tag, "tags[$index]")
      }

      payload.tools?.forEachIndexed { index, tool ->
        requireNotBlank(tool, "tools[$index]")
      }
    }

    val uuid = call.requireUuidParam("id")
    val recipe = recipeService.editRecipe(uuid, payload)

    call.respond(recipe)
  }

  patch(path = "/api/recipes/notes/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val recieved: String? = call.receiveNullable()

    val uuid = call.requireUuidParam("id")
    recipeService.updateRecipeNotes(uuid, recieved)
    call.respond(HttpStatusCode.NoContent)
  }

  delete(path = "/api/recipes/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val uuid = call.requireUuidParam("id")

    recipeService.deleteRecipe(uuid)
    call.respond(HttpStatusCode.NoContent)
  }
}
