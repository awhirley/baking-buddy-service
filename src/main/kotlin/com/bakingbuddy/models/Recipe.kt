package com.bakingbuddy.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val description: String,
    val createdAt: String?,
)