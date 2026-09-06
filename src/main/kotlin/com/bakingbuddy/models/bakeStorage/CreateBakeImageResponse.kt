package com.bakingbuddy.models.bakeStorage

import kotlinx.serialization.Serializable

@Serializable
data class CreateBakeImageResponse(
  val path: String,
  val imageUrl: String,
)
