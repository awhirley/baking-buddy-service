package com.bakingbuddy.api.routes

import com.bakingbuddy.models.ingredients.CreateIngredientPayload
import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.ingredients.UpdateIngredientPayload
import com.bakingbuddy.models.instructions.Instruction
import com.bakingbuddy.models.recipes.CreateRecipePayload
import com.bakingbuddy.models.recipes.Recipe
import com.bakingbuddy.models.recipes.RecipeDetail
import com.bakingbuddy.plugins.configureStatusPages
import com.bakingbuddy.services.RecipeService
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.request.get
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
    routing { ingredientRoutes(recipeService) }
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
    order = null,
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
    order = null,
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

class IngredientRoutesTest {
  // ---------------------------------------------------------------
  // PATCH /api/ingredients/{id}
  // ---------------------------------------------------------------

  @Test
  fun `PATCH ingredient with a valid payload returns 200`() =
    testApplication {
      // Same double-receive() bug as the recipe route -- see note above.
      // Will 500 rather than 200 until fixed.
      val service = mockk<RecipeService>()
      val id = Uuid.random()
      val payload = UpdateIngredientPayload(name = "Flour", amount = "500g", notes = null)
      coEvery { service.updateIngredient(id, payload) } returns sampleIngredient(id)
      val client = setupTestApp(service)

      val response =
        client.patch("/api/ingredients/$id") {
          contentType(ContentType.Application.Json)
          setBody(payload)
        }

      response.status shouldBe HttpStatusCode.OK
      coVerify(exactly = 1) { service.updateIngredient(id, payload) }
    }

  @Test
  fun `PATCH ingredient with a blank amount returns a field error and never calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/ingredients/${Uuid.random()}") {
          contentType(ContentType.Application.Json)
          setBody(UpdateIngredientPayload(name = "Flour", amount = "  ", notes = null))
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"amount\""
      coVerify(exactly = 0) { service.updateIngredient(any(), any()) }
    }

  @Test
  fun `PATCH ingredient with a blank name returns a field error`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/ingredients/${Uuid.random()}") {
          contentType(ContentType.Application.Json)
          setBody(UpdateIngredientPayload(name = "  ", amount = "500g", notes = null))
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"name\""
    }

  @Test
  fun `PATCH ingredient with a malformed uuid returns 400`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/ingredients/not-a-uuid") {
          contentType(ContentType.Application.Json)
          setBody(UpdateIngredientPayload(name = "Flour", amount = "500g", notes = null))
        }

      response.status shouldBe HttpStatusCode.BadRequest
    }
}
