package com.bakingbuddy.models.bakes

import com.bakingbuddy.models.instructions.InstructionDeltaEntry
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class BakeInstructionPayload(
  val bakeInstructionId: Uuid,
  val initialDeltaValues: InstructionDeltaEntry,
  val updatedDeltaValues: BakeInstruction,
  val completedBakeDeltaId: Uuid?,
)

@Serializable
data class BakeInstruction(
  val updatedDescription: String,
  val updatedNotes: String?,
  val updatedOrder: Int,
)
