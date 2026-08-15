package com.bakingbuddy.plugins

import com.bakingbuddy.api.errors.ApiErrorBody
import com.bakingbuddy.api.errors.ApiErrorCode
import com.bakingbuddy.api.errors.ApiErrorResponse
import com.bakingbuddy.api.errors.ApiException
import com.bakingbuddy.api.errors.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            cause.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "error" to (cause.message ?: "Unknown error")
                )
            )
        }
    
        exception<ContentTransformationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid request body", "detail" to (cause.cause?.message ?: cause.message)),
            )
        }
        exception<SerializationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid request body", "detail" to cause.message),
            )
        }
        exception<ApiException> { call, cause ->
            val body = when (cause) {
                is ValidationException -> ApiErrorBody(
                    code = cause.code,
                    message = cause.message,
                    fieldErrors = cause.fieldErrors,
                    details = cause.details
                )
                else -> ApiErrorBody(
                    code = cause.code,
                    message = cause.message,
                    details = cause.details
                )
            }
            call.respond(cause.statusCode, ApiErrorResponse(body))
        }

        // Catch-all: never leak internal exception messages/stack traces
        // to the client. Log the real exception server-side.
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiErrorResponse(
                    ApiErrorBody(
                        code = ApiErrorCode.INTERNAL_ERROR,
                        message = "An unexpected error occurred"
                    )
                )
            )
        }
    }
}
