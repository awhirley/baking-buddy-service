package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class AddBakeInstructionPayload(
  val description: String,
  val notes: String? = null,
  val previousBakeInstructionId: Uuid? = null,
  val nextBakeInstructionId: Uuid? = null,
)
