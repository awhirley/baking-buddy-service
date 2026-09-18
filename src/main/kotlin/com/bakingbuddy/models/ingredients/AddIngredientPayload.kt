package com.bakingbuddy.models.ingredients

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class AddIngredientPayload(
  val amount: String,
  val name: String,
  val notes: String? = null,
  val previousIngredientId: Uuid? = null,
  val nextIngredientId: Uuid? = null,
)