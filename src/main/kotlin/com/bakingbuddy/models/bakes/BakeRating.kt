package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable

@Serializable
data class BakeRating(
  val overall: Int? = null,
  val taste: Int? = null,
  val texture: Int? = null,
  val riseStructure: Int? = null,
  val appearance: Int? = null,
  val difficulty: Int? = null,
)
