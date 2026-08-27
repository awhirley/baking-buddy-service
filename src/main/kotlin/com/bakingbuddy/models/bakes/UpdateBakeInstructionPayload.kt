package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class UpdateBakeInstructionPayload(
  val deltaId: Uuid,
  val description: String,
)
