package com.bakingbuddy.models.instructions

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInstructionPayload(
  val description: String,
  val notes: String?,
  val order: Int,
)
