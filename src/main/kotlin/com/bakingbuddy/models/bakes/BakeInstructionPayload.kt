package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class BakeInstructionPayload(
  val bakeInstructionId: Uuid,
  val instructionId: Uuid,
  val instructionDeltaId: Uuid?,
  val version: Int,
  val description: String,
  val notes: String?,
  val updatedDescription: String?,
  val updatedNotes: String?,
)
