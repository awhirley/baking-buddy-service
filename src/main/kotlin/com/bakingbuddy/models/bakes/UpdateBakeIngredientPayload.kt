package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable

@Serializable
data class UpdateBakeIngredientPayload(
  val amount: String,
  val name: String,
)
