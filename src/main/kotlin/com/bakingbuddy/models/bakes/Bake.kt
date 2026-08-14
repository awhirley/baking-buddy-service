package com.bakingbuddy.models.bakes

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Bake(
    val id: Uuid,
    val recipeId: Uuid,
    val details: BakeDetail,
    val ingredientVersions: List<BakeIngredientPayload>,
    val instructionVersions: List<BakeInstructionPayload>,
)