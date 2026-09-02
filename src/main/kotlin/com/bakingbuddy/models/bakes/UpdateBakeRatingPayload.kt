package com.bakingbuddy.models.bakes

import com.bakingbuddy.api.PatchField
import com.bakingbuddy.api.PatchFieldSerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class UpdateBakeRatingPayload(
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val overall: PatchField<Int> = PatchField.Absent,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val taste: PatchField<Int> = PatchField.Absent,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val texture: PatchField<Int> = PatchField.Absent,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val riseStructure: PatchField<Int> = PatchField.Absent,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val appearance: PatchField<Int> = PatchField.Absent,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val difficulty: PatchField<Int> = PatchField.Absent,
)
