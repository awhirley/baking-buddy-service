package com.bakingbuddy.api.routes

import com.bakingbuddy.services.FilterService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

@Suppress("ThrowsCount")
fun Route.filterRoutes(filterService: FilterService) {
  get(path = "/api/tools") {
    val tools = filterService.listTools()
    call.respond(tools)
  }

  get(path = "/api/tags") {
    val tags = filterService.listTags()
    call.respond(tags)
  }

  get(path = "/api/sourceTypes") {
    val sourceTypes = filterService.listSourceTypes()
    call.respond(sourceTypes)
  }

  get(path = "/api/sources") {
    val sources = filterService.listSources()
    call.respond(sources)
  }
}
