package com.bakingbuddy.models.instructions

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class InstructionDeltaEntry(
  val id: Uuid,
  val instructionId: Uuid,
  val version: Int,
  val description: String?,
  val order: Int,
  @Serializable(with = InstantSerializer::class) val createdAt: Instant,
)
