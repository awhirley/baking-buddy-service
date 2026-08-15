package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.services.DeltaService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlin.uuid.Uuid

fun Route.deltaRoutes(
    deltaService: DeltaService
) {
  get(path="/api/ingredients/history/{id}") {
    val id = call.parameters["id"]
      ?: throw BadRequestException("Path parameter 'id' must be provided")
  
    val ingredientHistory = deltaService.getIngredientHistory(Uuid.parse(id))
    call.respond(ingredientHistory)
  }
  
  get(path="/api/instructions/history/{id}") {
    val id = call.parameters["id"]
      ?: throw BadRequestException("Path parameter 'id' must be provided")
  
    val instructionHistory = deltaService.getInstructionHistory(Uuid.parse(id))
    call.respond(instructionHistory)
  }
}