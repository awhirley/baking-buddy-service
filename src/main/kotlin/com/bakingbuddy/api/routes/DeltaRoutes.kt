package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.requireUuidParam
import com.bakingbuddy.services.DeltaService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.deltaRoutes(deltaService: DeltaService) {
    get(path = "/api/ingredients/history/{id}") {
        val id =
            call.parameters["id"]
                ?: throw BadRequestException("Path parameter 'id' must be provided")

        val uuid = call.requireUuidParam("id")
        val ingredientHistory = deltaService.getIngredientHistory(uuid)
        call.respond(ingredientHistory)
    }

    get(path = "/api/instructions/history/{id}") {
        val id =
            call.parameters["id"]
                ?: throw BadRequestException("Path parameter 'id' must be provided")

        val uuid = call.requireUuidParam("id")
        val instructionHistory = deltaService.getInstructionHistory(uuid)
        call.respond(instructionHistory)
    }
}
