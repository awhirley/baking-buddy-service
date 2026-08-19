package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.requireUuidParam
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

  get(path = "/api/bakes/recipe/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val uuid = call.requireUuidParam("id")
    val bakes = bakeService.listBakes(uuid)

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

  patch(path = "/api/bakes") {
    bakeService.updateBake(call.receive())
    call.respond(HttpStatusCode.NoContent)
  }

  delete(path = "api/bakes/{id}") {
    val id =
      call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

    val uuid = call.requireUuidParam("id")
    bakeService.deleteBake(uuid)
    call.respond(HttpStatusCode.NoContent)
  }
}
