package com.bakingbuddy.routes

import com.bakingbuddy.services.BakeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlin.uuid.Uuid

fun Route.bakeRoutes(
    bakeService: BakeService
) {
  post(path = "/api/recipes") {
      val recipe = bakeService.createBake(call.receive())
      call.respond(HttpStatusCode.Created, recipe)
  }
}