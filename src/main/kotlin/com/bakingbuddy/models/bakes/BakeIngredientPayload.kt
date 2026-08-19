package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class BakeIngredientPayload(
  val ingredientId: Uuid,
  val version: Int,
)
