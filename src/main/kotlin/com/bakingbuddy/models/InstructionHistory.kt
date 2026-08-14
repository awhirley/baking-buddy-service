package com.bakingbuddy.models

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class InstructionHistory(
    val id: Uuid,
    val recipeId: Uuid,
    val bestVersion: Int,
    val history: List<InstructionDeltaEntry>
)
