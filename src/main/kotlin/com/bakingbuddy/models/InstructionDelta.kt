package com.bakingbuddy.models

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class InstructionDelta(
    val id: Uuid,
    val instructionId: Uuid,
    val version: Int,
    val description: String?,
    val notes: String?,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
)
