package com.bakingbuddy.plugins

import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot

fun Application.configureOpenApi() {
    routing {
        openAPI(path = "openapi") {
            info = OpenApiInfo(title = "My Ktor API", version = "1.0.0")
            source = OpenApiDocSource.Routing { routingRoot.descendants() }
        }
    }
}
