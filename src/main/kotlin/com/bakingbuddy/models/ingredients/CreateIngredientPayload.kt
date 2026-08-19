package com.bakingbuddy.models.ingredients

import kotlinx.serialization.Serializable

@Serializable
data class CreateIngredientPayload(
    val amount: String,
    val name: String,
)
