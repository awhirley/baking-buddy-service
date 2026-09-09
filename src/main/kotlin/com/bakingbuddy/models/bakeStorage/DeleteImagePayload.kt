package com.bakingbuddy.models.bakeStorage

import kotlinx.serialization.Serializable

@Serializable
data class DeleteImagePayload(
  val path: String,
)
