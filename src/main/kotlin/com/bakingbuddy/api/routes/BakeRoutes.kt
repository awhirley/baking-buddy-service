package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.requireUuidParam
import com.bakingbuddy.api.errors.validate
import com.bakingbuddy.models.bakes.CompleteBakePayload
import com.bakingbuddy.models.bakes.UpdateBakeIngredientPayload
import com.bakingbuddy.models.bakes.UpdateBakeInstructionPayload
import com.bakingbuddy.services.BakeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

@Suppress("ThrowsCount")
fun Route.bakeRoutes(bakeService: BakeService) {
  post(path = "/api/bakes/recipe/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val uuid = call.requireUuidParam("id")
    val bake = bakeService.createBake(uuid)
    call.respond(HttpStatusCode.Created, bake)
  }
  
  get(path = "/api/bakes") {
    val bakes = bakeService.listBakes()
    call.respond(bakes)
  }

  get(path = "/api/bakes/recipe/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val uuid = call.requireUuidParam("id")
    val bakes = bakeService.listBakesForRecipe(uuid)

    call.respond(bakes)
  }

  get(path = "/api/bakes/recipe/{id}/procedure") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val uuid = call.requireUuidParam("id")
    val bakes = bakeService.listBakesWithProcedure(uuid)

    call.respond(bakes)
  }

  get(path = "/api/bakes/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val uuid = call.requireUuidParam("id")
    val bake = bakeService.getBake(uuid)

    call.respond(bake)
  }

  patch(path = "/api/bakes") {
    bakeService.updateBake(call.receive())
    call.respond(HttpStatusCode.NoContent)
  }

  delete(path = "/api/bakes/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val uuid = call.requireUuidParam("id")
    bakeService.deleteBake(uuid)
    call.respond(HttpStatusCode.NoContent)
  }

  patch(path = "/api/bakes/{bake_id}/instruction") {
    val bakeId =
      call.parameters["bake_id"]
        ?: throw BadRequestException("Path parameter 'bake_id' must be provided")

    val bakeUuid = call.requireUuidParam("bake_id")
    val payload = call.receive<UpdateBakeInstructionPayload>()

    validate {
      requireNotBlank(payload.description, "description")
    }

    bakeService.updateBakeInstruction(bakeUuid, payload)
    call.respond(HttpStatusCode.NoContent)
  }

  patch(path = "/api/bakes/{bake_id}/ingredient") {
    val bakeId =
      call.parameters["bake_id"]
        ?: throw BadRequestException("Path parameter 'bake_id' must be provided")

    val bakeUuid = call.requireUuidParam("bake_id")

    val payload = call.receive<UpdateBakeIngredientPayload>()

    validate {
      requireNotBlank(payload.amount, "amount")
      requireNotBlank(payload.name, "name")
    }

    bakeService.updateBakeIngredient(bakeUuid, payload)
    call.respond(HttpStatusCode.NoContent)
  }

  patch(path = "/api/bakes/{bake_id}/complete") {
    val bakeId =
      call.parameters["bake_id"]
        ?: throw BadRequestException("Path parameter 'bake_id' must be provided")

    val bakeUuid = call.requireUuidParam("bake_id")
    val payload = call.receive<CompleteBakePayload>()

    bakeService.completeBake(bakeUuid, payload)
    call.respond(HttpStatusCode.NoContent)
  }
}
