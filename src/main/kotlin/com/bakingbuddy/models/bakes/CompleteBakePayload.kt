package com.bakingbuddy.models.bakes

import kotlinx.serialization.Serializable

@Serializable
data class CompleteBakePayload(
  val setDeltasAsBest: Boolean,
)
