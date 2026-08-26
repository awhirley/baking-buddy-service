package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class BakeInstructionPayload(
  val instructionId: Uuid,
  val updated: Boolean,
  val instructionDeltaId: Uuid?,
  val version: Int,
  val description: String,
)
