package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class UpdateBakeIngredientPayload(
  val deltaId: Uuid,
  val amount: String,
  val name: String,
)
