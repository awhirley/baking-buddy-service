package com.bakingbuddy.models.bakes

import com.bakingbuddy.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class UpdateBakePayload(
  val bakeId: Uuid,
  @Serializable(with = InstantSerializer::class) val startDatetime: Instant? = null,
  @Serializable(with = InstantSerializer::class) val endDatetime: Instant? = null,
  val elevation: Int? = null,
  val notes: String? = null,
)
