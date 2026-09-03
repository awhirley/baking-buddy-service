package com.bakingbuddy.models.bakes

import com.bakingbuddy.models.ingredients.IngredientDeltaEntry
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class BakeIngredientPayload(
  val bakeIngredientId: Uuid,
  val initialDeltaValues: IngredientDeltaEntry,
  val updatedDeltaValues: BakeIngredient,
  val completedBakeDeltaId: Uuid?,
)

@Serializable
data class BakeIngredient(
  val updatedAmount: String,
  val updatedName: String,
  val updatedNotes: String?,
  val updatedOrder: Int,
)
