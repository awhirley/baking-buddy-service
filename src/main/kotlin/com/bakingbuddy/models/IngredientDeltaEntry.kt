package com.bakingbuddy.models

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class IngredientDeltaEntry(
    val id: Uuid,
    val ingredientId: Uuid,
    val version: Int,
    val amount: String?,
    val name: String?,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
)
