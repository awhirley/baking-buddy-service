package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.requireUuidParam
import com.bakingbuddy.api.errors.validate
import com.bakingbuddy.models.instructions.EditInstructionPayload
import com.bakingbuddy.services.RecipeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch

fun Route.instructionRoutes(recipeService: RecipeService) {
  patch(path = "/api/instructions/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val payload = call.receive<EditInstructionPayload>()

    validate {
      requireNotBlank(payload.description, "description")
    }

    val uuid = call.requireUuidParam("id")
    val instruction = recipeService.editInstruction(uuid, payload)
    call.respond(instruction)
  }

  patch(path = "/api/instructions/notes/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val uuid = call.requireUuidParam("id")
    recipeService.updateInstructionNotes(uuid, call.receive())
    call.respond(HttpStatusCode.NoContent)
  }
}
