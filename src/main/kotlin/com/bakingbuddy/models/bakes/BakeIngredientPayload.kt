package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class BakeIngredientPayload(
    val instructionId: Uuid,
    val version: Int,
)