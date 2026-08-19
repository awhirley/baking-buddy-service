package com.bakingbuddy.models.ingredients

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class IngredientHistory(
    val id: Uuid,
    val recipeId: Uuid,
    val bestVersion: Int,
    val history: List<IngredientDeltaEntry>,
)
