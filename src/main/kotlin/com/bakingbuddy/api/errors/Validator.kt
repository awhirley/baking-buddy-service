package com.bakingbuddy.api.errors

import io.ktor.server.application.ApplicationCall
import kotlin.uuid.Uuid

class Validator {
  private val errors = mutableListOf<FieldError>()

  fun require(
    condition: Boolean,
    field: String,
    message: String,
  ) {
    if (!condition) errors.add(FieldError(field, message))
  }

  fun requireNotBlank(
    value: String?,
    field: String,
  ) {
    require(!value.isNullOrBlank(), field, "must not be blank")
  }

  fun requirePositive(
    value: Number?,
    field: String,
  ) {
    if (value != null) require(value.toDouble() > 0, field, "must be positive")
  }

  fun requireNotBlankIfPresent(
    value: String?,
    field: String,
  ) {
    if (value != null) require(value.isNotBlank(), field, "must not be blank")
  }

  fun finish() {
    if (errors.isNotEmpty()) throw ValidationException(errors.toList())
  }
}

fun validate(block: Validator.() -> Unit) {
  Validator().apply(block).finish()
}

@Suppress("SwallowedException")
fun ApplicationCall.requireUuidParam(name: String): Uuid {
  val raw = parameters[name] ?: throw BadRequestException("Path parameter '$name' must be provided")
  return try {
    Uuid.parse(raw)
  } catch (e: IllegalArgumentException) {
    throw BadRequestException("Path parameter '$name' must be a valid UUID")
  }
}
