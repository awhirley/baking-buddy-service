package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable

@Serializable
data class UpdateBakeInstructionPayload(
  val description: String,
)
