package com.bakingbuddy.api.errors

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
  val error: ApiErrorBody,
)

@Serializable
data class ApiErrorBody(
  val code: ApiErrorCode,
  val message: String, // human-readable summary
  val fieldErrors: List<FieldError> = emptyList(),
  val details: Map<String, String> = emptyMap(), // exception-specific context, e.g. version conflict info
)

@Serializable
data class FieldError(
  val field: String,
  val message: String,
)
