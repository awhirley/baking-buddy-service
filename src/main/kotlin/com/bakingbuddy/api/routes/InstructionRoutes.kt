package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.requireUuidParam
import com.bakingbuddy.api.errors.validate
import com.bakingbuddy.models.instructions.UpdateInstructionPayload
import com.bakingbuddy.services.RecipeService
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch

fun Route.instructionRoutes(recipeService: RecipeService) {
  patch(path = "/api/instructions/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val payload = call.receive<UpdateInstructionPayload>()

    validate {
      requireNotBlank(payload.description, "description")
    }

    val uuid = call.requireUuidParam("id")
    val instruction = recipeService.updateInstruction(uuid, payload)
    call.respond(instruction)
  }
}
