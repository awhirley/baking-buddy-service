package com.bakingbuddy.models.ingredients

import kotlinx.serialization.Serializable

@Serializable
data class UpdateIngredientPayload(
  val amount: String,
  val name: String,
  val notes: String?,
)
