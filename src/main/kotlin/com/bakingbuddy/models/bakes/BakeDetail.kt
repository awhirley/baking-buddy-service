package com.bakingbuddy.models.bakes

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class BakeDetail(
    val id: Uuid,
    val recipeId: Uuid,
    val results: String? = null,
    val elevation: Int? = null,
    val notes: String? = null,
    @Serializable(with = InstantSerializer::class) val date: Instant? = null,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
)