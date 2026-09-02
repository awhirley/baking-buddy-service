package com.bakingbuddy.models.bakes

import com.bakingbuddy.api.PatchField
import com.bakingbuddy.api.PatchFieldSerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class UpdateBakePayload(
  val bakeId: Uuid,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val elevation: PatchField<Int> = PatchField.Absent,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val notes: PatchField<String> = PatchField.Absent,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val ratings: PatchField<UpdateBakeRatingPayload> = PatchField.Absent,
)
