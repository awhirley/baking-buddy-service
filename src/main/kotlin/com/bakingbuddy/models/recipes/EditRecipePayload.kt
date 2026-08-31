package com.bakingbuddy.models.recipes

import kotlinx.serialization.Serializable

@Serializable
data class EditRecipePayload(
  val name: String? = null,
  val description: String? = null,
  val recipeSource: String? = null,
  val recipeSourceType: String? = null,
  val tags: List<String>? = null,
  val tools: List<String>? = null,
  val favorite: Boolean,
  val difficultyRating: Int? = null,
)
