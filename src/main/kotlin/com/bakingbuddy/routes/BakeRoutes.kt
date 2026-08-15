package com.bakingbuddy.routes

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

      if (id == null) {
        call.respondText(
          "Invalid recipe ID",
          status = io.ktor.http.HttpStatusCode.BadRequest
        )
        return@post
      }
        
      val bake = bakeService.createBake(Uuid.parse(id))
      call.respond(HttpStatusCode.Created, bake)
  }
  
  get(path = "/api/bakes/recipe/{id}") {
        val id = call.parameters["id"]

        if (id == null) {
            call.respondText(
                "Invalid recipe ID",
                status = io.ktor.http.HttpStatusCode.BadRequest
            )
            return@get
        }

        val bakes = bakeService.listBakes(Uuid.parse(id))

        call.respond(bakes)
    }
    
  get(path = "/api/bakes/recipe/{id}/procedure") {
      val id = call.parameters["id"]

      if (id == null) {
          call.respondText(
              "Invalid recipe ID",
              status = io.ktor.http.HttpStatusCode.BadRequest
          )
          return@get
      }

      val bakes = bakeService.listBakesWithProcedure(Uuid.parse(id))

      call.respond(bakes)
    }

  patch(path = "/api/bakes") {
    val bake = bakeService.updateBake( call.receive())
    call.respond(bake)
  }
  
  delete(path="api/bakes/{id}") {
    val id = call.parameters["id"]

    if (id == null) {
      call.respondText(
        "Invalid bake ID",
        status = io.ktor.http.HttpStatusCode.BadRequest
      )
      return@delete
    }
      
    bakeService.deleteBake(Uuid.parse(id))
    call.respond(true)
  }
}