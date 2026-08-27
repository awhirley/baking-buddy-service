package com.bakingbuddy.models.instructions

import kotlinx.serialization.Serializable

@Serializable
data class EditInstructionPayload(
  val description: String,
)
