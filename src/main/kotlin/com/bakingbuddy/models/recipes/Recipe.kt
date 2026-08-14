package com.bakingbuddy.models.recipes

import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.instructions.Instruction
import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Recipe(
    val id: Uuid,
    val details: RecipeDetail,
    val ingredients: List<Ingredient>,
    val instructions: List<Instruction>,
)
