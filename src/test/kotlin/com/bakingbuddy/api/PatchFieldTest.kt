package com.bakingbuddy.api

import com.bakingbuddy.api.PatchField
import com.bakingbuddy.models.bakes.UpdateBakePayload
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

class PatchFieldTest {
  @Test
  fun `omitted key decodes as Absent`() {
    val payload =
      Json.decodeFromString<UpdateBakePayload>(
        """{"bakeId": "550e8400-e29b-41d4-a716-446655440000"}""",
      )
    payload.notes shouldBe PatchField.Absent
  }

  @Test
  fun `explicit null decodes as Present(null)`() {
    val payload =
      Json.decodeFromString<UpdateBakePayload>(
        """{"bakeId": "550e8400-e29b-41d4-a716-446655440000", "notes": null}""",
      )
    payload.notes shouldBe PatchField.Present(null)
  }

  @Test
  fun `explicit value decodes as Present(value)`() {
    val payload =
      Json.decodeFromString<UpdateBakePayload>(
        """{"bakeId": "550e8400-e29b-41d4-a716-446655440000", "notes": "great bake"}""",
      )
    payload.notes shouldBe PatchField.Present("great bake")
  }
}
