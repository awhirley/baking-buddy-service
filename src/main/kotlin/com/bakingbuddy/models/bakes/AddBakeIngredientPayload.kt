package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class AddBakeIngredientPayload(
  val amount: String,
  val name: String,
  val notes: String? = null,
  val previousBakeIngredientId: Uuid? = null,
  val nextBakeIngredientId: Uuid? = null,
)
