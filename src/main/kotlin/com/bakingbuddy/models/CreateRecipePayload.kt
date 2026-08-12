package com.bakingbuddy.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateIngredientPayload(
    val amount: Double,
    val name: String,
)

@Serializable
data class CreateRecipePayload(
    val name: String,
    val description: String,
    val recipeSource: String?,
    val tags: List<String>?,
    val tools: List<String>?,
    val ingredients: List<CreateIngredientPayload>,
    val instructions: List<String>,
)