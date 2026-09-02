package com.bakingbuddy.models.bakes

import com.bakingbuddy.api.PatchField
import com.bakingbuddy.api.PatchFieldSerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class UpdateBakeInstructionPayload(
  val deltaId: Uuid,
  val description: String?,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val notes: PatchField<String> = PatchField.Absent,
)
