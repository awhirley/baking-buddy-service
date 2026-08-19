package com.bakingbuddy.models.bakes

import com.bakingbuddy.serializers.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.uuid.Uuid

@Serializable
data class UpdateBakePayload(
  val bakeId: Uuid,
  @Serializable(with = LocalDateSerializer::class) val date: LocalDate? = null,
  val results: String? = null,
  val elevation: Int? = null,
  val notes: String? = null,
  // TODO: Support these in the repsitory layer
  val ingredientVersions: List<BakeIngredientPayload>? = null,
  val instructionVersions: List<BakeInstructionPayload>? = null,
)
