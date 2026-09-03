package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.models.ingredients.IngredientDeltaEntry
import com.bakingbuddy.models.ingredients.IngredientHistory
import com.bakingbuddy.models.instructions.InstructionDeltaEntry
import com.bakingbuddy.models.instructions.InstructionHistory
import com.bakingbuddy.plugins.configureStatusPages
import com.bakingbuddy.services.DeltaService
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
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

private fun ApplicationTestBuilder.setupTestApp(deltaService: DeltaService): HttpClient {
  application {
    install(ContentNegotiation) { json() }
    configureStatusPages()
    routing { deltaRoutes(deltaService) }
  }
  return createClient {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json() }
  }
}

private fun sampleIngredientHistory(
  id: Uuid = Uuid.random(),
  recipeId: Uuid = Uuid.random(),
): IngredientHistory =
  IngredientHistory(
    id = id,
    recipeId = recipeId,
    bestVersion = 2,
    history =
      listOf(
        IngredientDeltaEntry(
          id = Uuid.random(),
          ingredientId = id,
          version = 1,
          amount = "400g",
          name = "Flour",
          createdAt = Instant.now(),
          order = 1,
        ),
        IngredientDeltaEntry(
          id = Uuid.random(),
          ingredientId = id,
          version = 2,
          amount = "500g",
          name = "Flour",
          createdAt = Instant.now(),
          order = 1,
        ),
      ),
  )

private fun sampleInstructionHistory(
  id: Uuid = Uuid.random(),
  recipeId: Uuid = Uuid.random(),
): InstructionHistory =
  InstructionHistory(
    id = id,
    recipeId = recipeId,
    bestVersion = 1,
    history =
      listOf(
        InstructionDeltaEntry(
          id = Uuid.random(),
          instructionId = id,
          version = 1,
          description = "Mix and knead",
          createdAt = Instant.now(),
          order = 1,
        ),
      ),
  )

class DeltaRoutesTest {
  // ---------------------------------------------------------------
  // GET /api/ingredients/history/{id}
  // ---------------------------------------------------------------

  @Test
  fun `GET ingredient history returns 200 when found`() =
    testApplication {
      val service = mockk<DeltaService>()
      val history = sampleIngredientHistory()
      coEvery { service.getIngredientHistory(history.id) } returns history
      val client = setupTestApp(service)

      val response = client.get("/api/ingredients/history/${history.id}")

      response.status shouldBe HttpStatusCode.OK
      coVerify(exactly = 1) { service.getIngredientHistory(history.id) }
    }

  @Test
  fun `GET ingredient history returns 404 when the ingredient does not exist`() =
    testApplication {
      // ASSUMPTION: getIngredientHistory() throws NotFoundException (via the
      // repository) rather than returning null, since IngredientHistory is
      // non-nullable and DeltaRoutes has no null-check like recipeRoutes does.
      // Update this test if DeltaRepositoryImpl signals not-found differently.
      val service = mockk<DeltaService>()
      val id = Uuid.random()
      coEvery { service.getIngredientHistory(id) } throws NotFoundException("Ingredient", id.toString())
      val client = setupTestApp(service)

      val response = client.get("/api/ingredients/history/$id")

      response.status shouldBe HttpStatusCode.NotFound
      response.bodyAsText() shouldContain id.toString()
    }

  @Test
  fun `GET ingredient history returns 400 for a malformed uuid and never calls the service`() =
    testApplication {
      val service = mockk<DeltaService>()
      val client = setupTestApp(service)

      val response = client.get("/api/ingredients/history/not-a-uuid")

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.getIngredientHistory(any()) }
    }

  // ---------------------------------------------------------------
  // GET /api/instructions/history/{id}
  // ---------------------------------------------------------------

  @Test
  fun `GET instruction history returns 200 when found`() =
    testApplication {
      val service = mockk<DeltaService>()
      val history = sampleInstructionHistory()
      coEvery { service.getInstructionHistory(history.id) } returns history
      val client = setupTestApp(service)

      val response = client.get("/api/instructions/history/${history.id}")

      response.status shouldBe HttpStatusCode.OK
      coVerify(exactly = 1) { service.getInstructionHistory(history.id) }
    }

  @Test
  fun `GET instruction history returns 404 when the instruction does not exist`() =
    testApplication {
      // Same not-found assumption as the ingredient-history test above.
      val service = mockk<DeltaService>()
      val id = Uuid.random()
      coEvery { service.getInstructionHistory(id) } throws NotFoundException("Instruction", id.toString())
      val client = setupTestApp(service)

      val response = client.get("/api/instructions/history/$id")

      response.status shouldBe HttpStatusCode.NotFound
      response.bodyAsText() shouldContain id.toString()
    }

  @Test
  fun `GET instruction history returns 400 for a malformed uuid and never calls the service`() =
    testApplication {
      val service = mockk<DeltaService>()
      val client = setupTestApp(service)

      val response = client.get("/api/instructions/history/not-a-uuid")

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.getInstructionHistory(any()) }
    }
}
