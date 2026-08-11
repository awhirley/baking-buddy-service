package com.bakingbuddy.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateRecipeRequest(
    val name: String,
    val description: String
)