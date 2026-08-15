package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.services.BakeService
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

fun Route.bakeRoutes(
    bakeService: BakeService
) {
  post(path = "/api/bakes/recipe/{id}") {
      val id = call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")
        
      val bake = bakeService.createBake(Uuid.parse(id))
      call.respond(HttpStatusCode.Created, bake)
  }
  
  get(path = "/api/bakes/recipe/{id}") {
      val id = call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

        val bakes = bakeService.listBakes(Uuid.parse(id))

        call.respond(bakes)
    }
    
  get(path = "/api/bakes/recipe/{id}/procedure") {
      val id = call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")

      val bakes = bakeService.listBakesWithProcedure(Uuid.parse(id))

      call.respond(bakes)
    }

  patch(path = "/api/bakes") {
    val response = bakeService.updateBake( call.receive())
    call.respond(response)
  }
  
  delete(path="api/bakes/{id}") {
    val id = call.parameters["id"]
        ?: throw BadRequestException("Path parameter 'id' must be provided")
      
    val response = bakeService.deleteBake(Uuid.parse(id))
    call.respond(response)
  }
}