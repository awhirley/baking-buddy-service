package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Bake(
  val id: Uuid,
  val recipeId: Uuid,
  val details: BakeDetail,
  val ingredientVersions: List<BakeIngredientPayload>,
  val instructionVersions: List<BakeInstructionPayload>,
)
