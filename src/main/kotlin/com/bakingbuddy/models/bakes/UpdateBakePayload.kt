package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class UpdateBakePayload(
  val bakeId: Uuid,
  val elevation: Int? = null,
  val notes: String? = null,
  val ratings: BakeRating? = null,
)
