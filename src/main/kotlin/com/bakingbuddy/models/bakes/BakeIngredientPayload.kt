package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class BakeIngredientPayload(
  val bakeIngredientId: Uuid,
  val ingredientId: Uuid,
  val ingredientDeltaId: Uuid?,
  val version: Int?,
  val amount: String,
  val name: String,
  val notes: String?,
)
