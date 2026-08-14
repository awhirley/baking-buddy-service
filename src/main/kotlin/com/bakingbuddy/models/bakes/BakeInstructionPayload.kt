package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class BakeInstructionPayload(
    val instructionId: Uuid,
    val version: Int,
)