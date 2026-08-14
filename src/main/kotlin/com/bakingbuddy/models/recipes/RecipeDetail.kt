package com.bakingbuddy.models.recipes

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class RecipeDetail(
    val id: Uuid,
    val name: String,
    val description: String?,
    val recipeSource: String?,
    val tags: List<String>?,
    val tools: List<String>?,
    val notes: String?,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
)