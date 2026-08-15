package com.bakingbuddy.models.bakes

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class UpdateBakePayload(
    val recipeId: Uuid,
    @Serializable(with = InstantSerializer::class) val date: Instant?,
    val results: String?,
    val elevation: Int?,
    val notes: String? = null,
    val ingredientVersions: List<BakeIngredientPayload>?,
    val instructionVersions: List<BakeInstructionPayload>?,
)
