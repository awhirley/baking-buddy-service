package com.bakingbuddy.models

import kotlinx.serialization.Serializable

@Serializable
data class EditRecipePayload(
    val name: String? = null,
    val description: String? = null,
    val recipeSource: String? = null,
    val tags: List<String>? = null,
    val tools: List<String>? = null,
)

@Serializable
data class EditIngredientPayload(
    val amount: String,
    val name: String,
    val setAsBestVersion: Boolean? = false,
)

@Serializable
data class EditInstructionPayload(
    val description: String,
    val setAsBestVersion: Boolean? = false,
)