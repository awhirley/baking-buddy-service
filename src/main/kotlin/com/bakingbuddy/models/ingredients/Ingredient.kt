package com.bakingbuddy.models.ingredients

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Ingredient(
  val id: Uuid,
  val recipeId: Uuid,
  val bestVersion: Int,
  val notes: String?,
  @Serializable(with = InstantSerializer::class) val createdAt: Instant,
  val amount: String,
  val name: String,
)
