package com.bakingbuddy.api.routes

import com.bakingbuddy.models.ingredients.CreateIngredientPayload
import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.instructions.Instruction
import com.bakingbuddy.models.instructions.UpdateInstructionPayload
import com.bakingbuddy.models.recipes.CreateRecipePayload
import com.bakingbuddy.models.recipes.Recipe
import com.bakingbuddy.models.recipes.RecipeDetail
import com.bakingbuddy.plugins.configureStatusPages
import com.bakingbuddy.services.RecipeService
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.request.patch
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

private fun ApplicationTestBuilder.setupTestApp(recipeService: RecipeService): HttpClient {
  application {
    install(ContentNegotiation) { json() }
    configureStatusPages()
    routing { instructionRoutes(recipeService) }
  }
  // The default `client` provided by testApplication has no JSON support --
  // it needs its own ContentNegotiation to serialize setBody(someDataClass).
  return createClient {
    install(ClientContentNegotiation) { json() }
  }
}

private fun sampleRecipe(id: Uuid = Uuid.random()): Recipe =
  Recipe(
    id = id,
    details =
      RecipeDetail(
        id = id,
        name = "Sourdough Loaf",
        description = "Basic sourdough",
        recipeSource = null,
        recipeSourceType = null,
        tags = null,
        tools = null,
        notes = null,
        createdAt = Instant.now(),
        openBakeId = null,
        favorite = false,
        difficultyRating = 3,
      ),
    ingredients = emptyList(),
    instructions = emptyList(),
  )

private fun sampleRecipeDetail(id: Uuid = Uuid.random()): RecipeDetail =
  RecipeDetail(
    id = id,
    name = "Sourdough Loaf",
    description = "Basic sourdough",
    recipeSource = null,
    recipeSourceType = null,
    tags = null,
    tools = null,
    notes = null,
    createdAt = Instant.now(),
    openBakeId = null,
    favorite = false,
    difficultyRating = 3,
  )

private fun sampleIngredient(
  id: Uuid = Uuid.random(),
  recipeId: Uuid = Uuid.random(),
): Ingredient =
  Ingredient(
    id = id,
    recipeId = recipeId,
    bestVersion = 1,
    notes = null,
    createdAt = Instant.now(),
    amount = "500g",
    name = "Flour",
    order = 1,
  )

private fun sampleInstruction(
  id: Uuid = Uuid.random(),
  recipeId: Uuid = Uuid.random(),
): Instruction =
  Instruction(
    id = id,
    recipeId = recipeId,
    bestVersion = 1,
    notes = null,
    createdAt = Instant.now(),
    description = "Mix and knead",
    order = 1,
  )

private fun validCreateRecipePayload() =
  CreateRecipePayload(
    name = "Sourdough Loaf",
    description = "Basic sourdough",
    recipeSource = null,
    recipeSourceType = null,
    tags = null,
    tools = null,
    ingredients = listOf(CreateIngredientPayload(name = "Flour", amount = "500g")),
    instructions = listOf("Mix and knead"),
  )

class InstructionRoutesTest {
  // ---------------------------------------------------------------
  // PATCH /api/instructions/{id}
  // ---------------------------------------------------------------

  @Test
  fun `PATCH instruction with a valid payload returns 200`() =
    testApplication {
      // Same double-receive() bug -- see note on the recipe PATCH test.
      val service = mockk<RecipeService>()
      val id = Uuid.random()
      val payload = UpdateInstructionPayload(description = "Mix, knead, and rest", notes = null, order = 1)
      coEvery { service.updateInstruction(id, payload) } returns sampleInstruction(id)
      val client = setupTestApp(service)

      val response =
        client.patch("/api/instructions/$id") {
          contentType(ContentType.Application.Json)
          setBody(payload)
        }

      response.status shouldBe HttpStatusCode.OK
      coVerify(exactly = 1) { service.updateInstruction(id, payload) }
    }

  @Test
  fun `PATCH instruction with a blank description returns a field error and never calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/instructions/${Uuid.random()}") {
          contentType(ContentType.Application.Json)
          setBody(UpdateInstructionPayload(description = "   ", notes = null, order = 1))
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"description\""
      coVerify(exactly = 0) { service.updateInstruction(any(), any()) }
    }
}
