package com.bakingbuddy.models.bakes

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class BakeDetail(
    val id: Uuid,
    val recipeId: Uuid,
    val results: String,
    val elevation: Int,
    val notes: String?,
    @Serializable(with = InstantSerializer::class) val date: Instant,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
)