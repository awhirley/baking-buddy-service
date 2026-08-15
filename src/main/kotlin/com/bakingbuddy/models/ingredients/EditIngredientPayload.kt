package com.bakingbuddy.models.ingredients

import kotlinx.serialization.Serializable

@Serializable
data class EditIngredientPayload(
    val amount: String? = null,
    val name: String? = null,
    val setAsBestVersion: Boolean? = false,
)
