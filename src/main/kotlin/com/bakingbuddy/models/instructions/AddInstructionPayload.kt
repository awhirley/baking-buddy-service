package com.bakingbuddy.models.instructions

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class AddInstructionPayload(
  val description: String,
  val notes: String? = null,
  val previousInstructionId: Uuid? = null,
  val nextInstructionId: Uuid? = null,
)
