package com.bakingbuddy.models.bakes

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class BakeDetail(
  val id: Uuid,
  val recipeName: String,
  val recipeId: Uuid,
  val elevation: Int? = null,
  val notes: String? = null,
  val ratings: BakeRating? = null,
  @Serializable(with = InstantSerializer::class) val createdAt: Instant,
  @Serializable(with = InstantSerializer::class) val startDatetime: Instant?,
  @Serializable(with = InstantSerializer::class) val endDatetime: Instant? = null,
)
