package com.bakingbuddy.models.bakeStorage

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class BakeImage(
  val bakeId: Uuid,
  val path: String,
  val imageUrl: String?,
  @Serializable(with = InstantSerializer::class) val createdAt: Instant,
)
