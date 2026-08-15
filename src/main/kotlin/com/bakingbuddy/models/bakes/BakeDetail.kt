package com.bakingbuddy.models.bakes

import com.bakingbuddy.serializers.InstantSerializer
import com.bakingbuddy.serializers.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class BakeDetail(
    val id: Uuid,
    val recipeId: Uuid,
    val results: String? = null,
    val elevation: Int? = null,
    val notes: String? = null,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate? = null,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
)