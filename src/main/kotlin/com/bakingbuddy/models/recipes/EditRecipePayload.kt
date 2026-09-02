package com.bakingbuddy.models.recipes

import com.bakingbuddy.api.PatchField
import com.bakingbuddy.api.PatchFieldNonNull
import com.bakingbuddy.api.PatchFieldNonNullSerializer
import com.bakingbuddy.api.PatchFieldSerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class EditRecipePayload(
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldNonNullSerializer::class)
  val name: PatchFieldNonNull<String> = PatchFieldNonNull.Absent,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val description: PatchField<String> = PatchField.Absent,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val recipeSource: PatchField<String> = PatchField.Absent,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val recipeSourceType: PatchField<String> = PatchField.Absent,
  val tags: List<String>? = null,
  val tools: List<String>? = null,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldNonNullSerializer::class)
  val favorite: PatchFieldNonNull<Boolean> = PatchFieldNonNull.Absent,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Serializable(with = PatchFieldSerializer::class)
  val difficultyRating: PatchField<Int> = PatchField.Absent,
)
