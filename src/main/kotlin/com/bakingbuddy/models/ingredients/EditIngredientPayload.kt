package com.bakingbuddy.models.ingredients

import kotlinx.serialization.Serializable

@Serializable
data class EditIngredientPayload(
  val amount: String,
  val name: String,
)
