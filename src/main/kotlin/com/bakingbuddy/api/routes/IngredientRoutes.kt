package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.requireUuidParam
import com.bakingbuddy.api.errors.validate
import com.bakingbuddy.models.ingredients.AddIngredientPayload
import com.bakingbuddy.models.ingredients.UpdateIngredientPayload
import com.bakingbuddy.services.RecipeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

fun Route.ingredientRoutes(recipeService: RecipeService) {
  patch(path = "/api/ingredients/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val payload = call.receive<UpdateIngredientPayload>()

    validate {
      requireNotBlank(payload.amount, "amount")
      requireNotBlank(payload.name, "name")
    }

    val uuid = call.requireUuidParam("id")
    val ingredient = recipeService.updateIngredient(uuid, payload)
    call.respond(ingredient)
  }

  post(path = "/api/recipes/{recipeId}/ingredients") {
    val recipeId =
      call.parameters["recipeId"]
        ?: throw BadRequestException("Path parameter 'recipeId' must be provided")

    val payload = call.receive<AddIngredientPayload>()

    validate {
      requireNotBlank(payload.amount, "amount")
      requireNotBlank(payload.name, "name")
    }

    val recipeUuid = call.requireUuidParam("recipeId")
    val ingredient = recipeService.addIngredient(recipeUuid, payload)
    call.respond(HttpStatusCode.Created, ingredient)
  }

  delete(path = "/api/ingredients/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val uuid = call.requireUuidParam("id")
    recipeService.omitIngredient(uuid)
    call.respond(HttpStatusCode.NoContent)
  }
}
