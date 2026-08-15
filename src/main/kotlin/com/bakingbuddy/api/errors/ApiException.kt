package com.bakingbuddy.api.errors

import io.ktor.http.HttpStatusCode

sealed class ApiException(
    val statusCode: HttpStatusCode,
    val code: ApiErrorCode,
    override val message: String,
    val details: Map<String, String> = emptyMap()
) : Exception(message)

class NotFoundException(
    resource: String,
    id: String
) : ApiException(
    HttpStatusCode.NotFound,
    ApiErrorCode.NOT_FOUND,
    "$resource with id $id was not found"
)

class ValidationException(
    val fieldErrors: List<FieldError>
) : ApiException(
    HttpStatusCode.BadRequest,
    ApiErrorCode.VALIDATION_ERROR,
    "Request failed validation"
)

class ConflictException(
    message: String,
    details: Map<String, String> = emptyMap()
) : ApiException(
    HttpStatusCode.Conflict,
    ApiErrorCode.CONFLICT,
    message,
    details
)

// Malformed request that isn't a field-validation failure — e.g. an
// unparseable path/query parameter. Distinct from ValidationException
// because there's no "field" being validated, just a broken request.
class BadRequestException(
    message: String
) : ApiException(
    HttpStatusCode.BadRequest,
    ApiErrorCode.BAD_REQUEST,
    message
)

// Well-formed, passes field validation, but violates a business rule —
// e.g. reverting to a best_version that doesn't exist for this
// ingredient. Distinct from ValidationException (shape) and
// ConflictException (competing state): this is about semantic validity.
class UnprocessableEntityException(
    message: String,
    details: Map<String, String> = emptyMap()
) : ApiException(
    HttpStatusCode.UnprocessableEntity,
    ApiErrorCode.UNPROCESSABLE_ENTITY,
    message,
    details
)

// Data integrity issue — e.g. an ingredient/instruction missing its
// best_version delta row. This is a 500, not a 400/404: it's not the
// client's fault and not a normal "missing resource" case, it means
// something is wrong in the data itself.
class DataIntegrityException(
    message: String
) : ApiException(
    HttpStatusCode.InternalServerError,
    ApiErrorCode.DATA_INTEGRITY_ERROR,
    message
)