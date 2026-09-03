package com.bakingbuddy.api.routes

import com.bakingbuddy.api.PatchField
import com.bakingbuddy.models.bakes.Bake
import com.bakingbuddy.models.bakes.BakeDetail
import com.bakingbuddy.models.bakes.UpdateBakePayload
import com.bakingbuddy.models.bakes.UpdateBakeRatingPayload
import com.bakingbuddy.plugins.configureStatusPages
import com.bakingbuddy.services.BakeService
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlin.test.Test
import kotlin.uuid.Uuid
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private fun ApplicationTestBuilder.setupTestApp(bakeService: BakeService): HttpClient {
  application {
    install(ContentNegotiation) { json() }
    configureStatusPages()
    routing { bakeRoutes(bakeService) }
  }
  return createClient {
    install(ClientContentNegotiation) { json() }
  }
}

private fun sampleBakeDetail(
  id: Uuid = Uuid.random(),
  recipeId: Uuid = Uuid.random(),
): BakeDetail =
  BakeDetail(
    id = id,
    recipeId = recipeId,
    elevation = 5280,
    notes = null,
    createdAt = Instant.now(),
    startDatetime = Instant.now(),
    endDatetime = null,
  )

private fun sampleBake(
  id: Uuid = Uuid.random(),
  recipeId: Uuid = Uuid.random(),
): Bake =
  Bake(
    id = id,
    recipeId = recipeId,
    details = sampleBakeDetail(id = id, recipeId = recipeId),
    ingredientVersions = emptyList(),
    instructionVersions = emptyList(),
  )

private fun validUpdateBakePayload(bakeId: Uuid = Uuid.random()) =
  UpdateBakePayload(
    bakeId = bakeId,
    elevation = PatchField.Present(5280),
    notes = PatchField.Present("Great bake!"),
    ratings =
      PatchField.Present(
        UpdateBakeRatingPayload(
          overall = PatchField.Present(5),
          taste = PatchField.Present(5),
          texture = PatchField.Present(5),
          riseStructure = PatchField.Present(5),
          appearance = PatchField.Present(5),
          difficulty = PatchField.Present(5),
        ),
      ),
  )

class BakeRoutesTest {
  // ---------------------------------------------------------------
  // POST /api/bakes/recipe/{id}
  // ---------------------------------------------------------------

  @Test
  fun `POST bake for a recipe returns 201 and calls the service`() =
    testApplication {
      val service = mockk<BakeService>()
      val recipeId = Uuid.random()
      val bake = sampleBake(recipeId = recipeId)
      coEvery { service.createBake(recipeId) } returns bake
      val client = setupTestApp(service)

      val response = client.post("/api/bakes/recipe/$recipeId")

      response.status shouldBe HttpStatusCode.Created
      coVerify(exactly = 1) { service.createBake(recipeId) }
    }

  @Test
  fun `POST bake with a malformed recipe uuid returns 400 and never calls the service`() =
    testApplication {
      val service = mockk<BakeService>()
      val client = setupTestApp(service)

      val response = client.post("/api/bakes/recipe/not-a-uuid")

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.createBake(any()) }
    }

  // ---------------------------------------------------------------
  // GET /api/bakes/recipe/{id}
  // ---------------------------------------------------------------

  @Test
  fun `GET bakes for a recipe returns 200 with the service's list`() =
    testApplication {
      val service = mockk<BakeService>()
      val recipeId = Uuid.random()
      coEvery { service.listBakesForRecipe(recipeId) } returns
        listOf(sampleBakeDetail(recipeId = recipeId), sampleBakeDetail(recipeId = recipeId))
      val client = setupTestApp(service)

      val response = client.get("/api/bakes/recipe/$recipeId")

      response.status shouldBe HttpStatusCode.OK
      coVerify(exactly = 1) { service.listBakesForRecipe(recipeId) }
    }

  @Test
  fun `GET bakes for a recipe returns 200 with an empty list when there are no bakes`() =
    testApplication {
      val service = mockk<BakeService>()
      val recipeId = Uuid.random()
      coEvery { service.listBakesForRecipe(recipeId) } returns emptyList()
      val client = setupTestApp(service)

      val response = client.get("/api/bakes/recipe/$recipeId")

      response.status shouldBe HttpStatusCode.OK
      response.bodyAsText() shouldBe "[]"
    }

  @Test
  fun `GET bakes for a recipe returns 400 for a malformed uuid and never calls the service`() =
    testApplication {
      val service = mockk<BakeService>()
      val client = setupTestApp(service)

      val response = client.get("/api/bakes/recipe/not-a-uuid")

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.listBakesForRecipe(any()) }
    }

  // ---------------------------------------------------------------
  // GET /api/bakes/recipe/{id}/procedure
  // ---------------------------------------------------------------

  @Test
  fun `GET bakes with procedure returns 200 with the service's list`() =
    testApplication {
      val service = mockk<BakeService>()
      val recipeId = Uuid.random()
      coEvery { service.listBakesWithProcedure(recipeId) } returns listOf(sampleBake(recipeId = recipeId))
      val client = setupTestApp(service)

      val response = client.get("/api/bakes/recipe/$recipeId/procedure")

      response.status shouldBe HttpStatusCode.OK
      coVerify(exactly = 1) { service.listBakesWithProcedure(recipeId) }
    }

  @Test
  fun `GET bakes with procedure returns 400 for a malformed uuid and never calls the service`() =
    testApplication {
      val service = mockk<BakeService>()
      val client = setupTestApp(service)

      val response = client.get("/api/bakes/recipe/not-a-uuid/procedure")

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.listBakesWithProcedure(any()) }
    }

  // ---------------------------------------------------------------
  // PATCH /api/bakes
  // ---------------------------------------------------------------

  @Test
  fun `PATCH bake with a valid payload calls updateBake and returns 204`() =
    testApplication {
      val service = mockk<BakeService>()
      val payload = validUpdateBakePayload()
      coEvery { service.updateBake(payload) } returns Unit
      val client = setupTestApp(service)

      val response =
        client.patch("/api/bakes") {
          contentType(ContentType.Application.Json)
          setBody(payload)
        }

      response.status shouldBe HttpStatusCode.NoContent
      coVerify(exactly = 1) { service.updateBake(payload) }
    }

  @Test
  fun `PATCH bake with malformed JSON returns 400 Invalid request body and never calls the service`() =
    testApplication {
      val service = mockk<BakeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/bakes") {
          contentType(ContentType.Application.Json)
          setBody("{ not valid json")
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "Invalid request body"
      coVerify(exactly = 0) { service.updateBake(any()) }
    }

  @Test
  fun `PATCH bake with a body missing bakeId returns 400 and never calls the service`() =
    testApplication {
      // UpdateBakePayload.bakeId has no default, so a body omitting it should
      // fail deserialization (caught as a 400 by the same handler as malformed JSON)
      // rather than reach the service.
      val service = mockk<BakeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/bakes") {
          contentType(ContentType.Application.Json)
          setBody("""{"notes":"Golden crust"}""")
        }

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.updateBake(any()) }
    }

  // ---------------------------------------------------------------
  // DELETE /api/bakes/{id}
  // ---------------------------------------------------------------

  @Test
  fun `DELETE bake with a valid id returns 204 and calls the service`() =
    testApplication {
      // KNOWN BUG: the route is registered as delete(path = "api/bakes/{id}"),
      // missing the leading slash that every other route in this file (and in
      // RecipeRoutes/DeltaRoutes) has. Ktor's routing tree is slash-anchored,
      // so this most likely does NOT match "/api/bakes/{id}" as intended and
      // this test will fail (404) until the route path is fixed to
      // "/api/bakes/{id}".
      val service = mockk<BakeService>()
      val id = Uuid.random()
      coEvery { service.deleteBake(id) } returns Unit
      val client = setupTestApp(service)

      val response = client.delete("/api/bakes/$id")

      response.status shouldBe HttpStatusCode.NoContent
      coVerify(exactly = 1) { service.deleteBake(id) }
    }

  @Test
  fun `DELETE bake with a malformed uuid returns 400 and never calls the service`() =
    testApplication {
      val service = mockk<BakeService>()
      val client = setupTestApp(service)

      val response = client.delete("/api/bakes/not-a-uuid")

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.deleteBake(any()) }
    }
}
