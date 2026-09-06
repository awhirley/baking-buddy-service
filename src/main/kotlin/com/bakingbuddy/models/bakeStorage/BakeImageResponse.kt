package com.bakingbuddy.models.bakeStorage

import kotlinx.serialization.Serializable

@Serializable
data class BakeImageResponse(
  val path: String,
  val imageUrl: String,
)