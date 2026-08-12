package com.bakingbuddy.models

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Recipe(
    val id: Uuid,
    val name: String,
    val description: String?,
    val recipeSource: String?,
    val tags: List<String>?,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
)
