package com.bakingbuddy.models.bakes

import com.bakingbuddy.api.PatchField
import com.bakingbuddy.api.PatchFieldSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class UpdateBakePayload(
  val bakeId: Uuid,
  @Serializable(with = PatchFieldSerializer::class)
  val elevation: PatchField<Int> = PatchField.Absent,
  @Serializable(with = PatchFieldSerializer::class)
  val notes: PatchField<String> = PatchField.Absent,
  @Serializable(with = PatchFieldSerializer::class)
  val ratings: PatchField<UpdateBakeRatingPayload> = PatchField.Absent
)