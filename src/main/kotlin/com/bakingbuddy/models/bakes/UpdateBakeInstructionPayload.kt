package com.bakingbuddy.models.bakes

import com.bakingbuddy.api.PatchField
import com.bakingbuddy.api.PatchFieldSerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class UpdateBakeInstructionPayload(
  val bakeInstructionId: Uuid,
  val description: String,
  val order: Int,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val notes: PatchField<String> = PatchField.Absent,
)
