package com.bakingbuddy.api.routes

import com.bakingbuddy.models.ingredients.CreateIngredientPayload
import com.bakingbuddy.models.ingredients.EditIngredientPayload
import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.instructions.EditInstructionPayload
import com.bakingbuddy.models.instructions.Instruction
import com.bakingbuddy.models.recipes.CreateRecipePayload
import com.bakingbuddy.models.recipes.EditRecipePayload
import com.bakingbuddy.models.recipes.Recipe
import com.bakingbuddy.models.recipes.RecipeDetail
import com.bakingbuddy.plugins.configureStatusPages
import com.bakingbuddy.services.RecipeService
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
import io.mockk.slot
import org.junit.jupiter.api.Disabled
import java.time.Instant
import kotlin.test.Test
import kotlin.uuid.Uuid
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

/**
 * NOTE: RecipeService is a concrete class (not an interface). mockk<RecipeService>()
 * still works without invoking the real constructor (MockK bypasses it via Objenesis),
 * so the real RecipeRepositoryImpl() it constructs internally is never touched.
 *
 * Error bodies are asserted on loosely (via shouldContain on the raw JSON text,
 * e.g. `"field":"name"`) rather than by deserializing into ApiErrorResponse/ApiErrorBody,
 * since those two class definitions weren't provided. Swap to typed deserialization
 * if you want stricter checks.
 */

private fun ApplicationTestBuilder.setupTestApp(recipeService: RecipeService): HttpClient {
  application {
    install(ContentNegotiation) { json() }
    configureStatusPages()
    routing { recipeRoutes(recipeService) }
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
        tags = null,
        tools = null,
        notes = null,
        createdAt = Instant.now(),
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
    tags = null,
    tools = null,
    notes = null,
    createdAt = Instant.now(),
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
  )

private fun validCreateRecipePayload() =
  CreateRecipePayload(
    name = "Sourdough Loaf",
    description = "Basic sourdough",
    recipeSource = null,
    tags = null,
    tools = null,
    ingredients = listOf(CreateIngredientPayload(name = "Flour", amount = "500g")),
    instructions = listOf("Mix and knead"),
  )

class RecipeRoutesTest {
  // ---------------------------------------------------------------
  // GET /api/recipes/{id}
  // ---------------------------------------------------------------

  @Test
  fun `GET recipe by id returns 200 when found`() =
    testApplication {
      val service = mockk<RecipeService>()
      val recipe = sampleRecipe()
      coEvery { service.getRecipe(recipe.id) } returns recipe
      val client = setupTestApp(service)

      val response = client.get("/api/recipes/${recipe.id}")

      response.status shouldBe HttpStatusCode.OK
    }

  @Test
  fun `GET recipe by id returns 404 when service returns null`() =
    testApplication {
      val service = mockk<RecipeService>()
      val id = Uuid.random()
      coEvery { service.getRecipe(id) } returns null
      val client = setupTestApp(service)

      val response = client.get("/api/recipes/$id")

      response.status shouldBe HttpStatusCode.NotFound
      response.bodyAsText() shouldContain id.toString()
    }

  @Test
  fun `GET recipe by id returns 400 for a malformed uuid and never calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response = client.get("/api/recipes/not-a-uuid")

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.getRecipe(any()) }
    }

  // ---------------------------------------------------------------
  // GET /api/recipes
  // ---------------------------------------------------------------

  @Test
  fun `GET recipes returns 200 with the service's list`() =
    testApplication {
      val service = mockk<RecipeService>()
      coEvery { service.listRecipes() } returns listOf(sampleRecipeDetail(), sampleRecipeDetail())
      val client = setupTestApp(service)

      val response = client.get("/api/recipes")

      response.status shouldBe HttpStatusCode.OK
    }

  // ---------------------------------------------------------------
  // POST /api/recipes
  // ---------------------------------------------------------------

  @Test
  fun `POST recipe with a valid payload returns 201 and calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val payload = validCreateRecipePayload()
      coEvery { service.createRecipe(payload) } returns sampleRecipe()
      val client = setupTestApp(service)

      val response =
        client.post("/api/recipes") {
          contentType(ContentType.Application.Json)
          setBody(payload)
        }

      response.status shouldBe HttpStatusCode.Created
      coVerify(exactly = 1) { service.createRecipe(payload) }
    }

  @Test
  fun `POST recipe with malformed JSON returns 400 Invalid request body`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.post("/api/recipes") {
          contentType(ContentType.Application.Json)
          setBody("{ not valid json")
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "Invalid request body"
    }

  @Test
  fun `POST recipe with a blank name returns 400 with a field error and never calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.post("/api/recipes") {
          contentType(ContentType.Application.Json)
          setBody(validCreateRecipePayload().copy(name = "   "))
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"name\""
      coVerify(exactly = 0) { service.createRecipe(any()) }
    }

  @Test
  fun `POST recipe with no ingredients returns a field error for ingredients`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.post("/api/recipes") {
          contentType(ContentType.Application.Json)
          setBody(validCreateRecipePayload().copy(ingredients = emptyList()))
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"ingredients\""
    }

  @Test
  fun `POST recipe with no instructions returns a field error for instructions`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.post("/api/recipes") {
          contentType(ContentType.Application.Json)
          setBody(validCreateRecipePayload().copy(instructions = emptyList()))
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"instructions\""
    }

  @Test
  fun `POST recipe with a blank ingredient name reports an indexed field path`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.post("/api/recipes") {
          contentType(ContentType.Application.Json)
          setBody(
            validCreateRecipePayload().copy(
              ingredients = listOf(CreateIngredientPayload(name = "  ", amount = "500g")),
            ),
          )
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"ingredients[0].name\""
    }

  @Test
  fun `POST recipe with a blank ingredient amount reports an indexed field path`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.post("/api/recipes") {
          contentType(ContentType.Application.Json)
          setBody(
            validCreateRecipePayload().copy(
              ingredients = listOf(CreateIngredientPayload(name = "Flour", amount = " ")),
            ),
          )
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"ingredients[0].amount\""
    }

  @Test
  fun `POST recipe with a blank instruction reports an indexed field path`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.post("/api/recipes") {
          contentType(ContentType.Application.Json)
          setBody(validCreateRecipePayload().copy(instructions = listOf("   ")))
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"instructions[0]\""
    }

  @Test
  fun `POST recipe with a blank tag reports an indexed field path`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.post("/api/recipes") {
          contentType(ContentType.Application.Json)
          setBody(validCreateRecipePayload().copy(tags = listOf("sourdough", "  ")))
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"tags[1]\""
    }

  @Test
  fun `POST recipe with null tags does not trigger a tags validation error`() =
    testApplication {
      val service = mockk<RecipeService>()
      val payload = validCreateRecipePayload().copy(tags = null)
      coEvery { service.createRecipe(payload) } returns sampleRecipe()
      val client = setupTestApp(service)

      val response =
        client.post("/api/recipes") {
          contentType(ContentType.Application.Json)
          setBody(payload)
        }

      response.status shouldBe HttpStatusCode.Created
    }

  @Test
  fun `POST recipe collects multiple field errors in a single response`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.post("/api/recipes") {
          contentType(ContentType.Application.Json)
          setBody(
            validCreateRecipePayload().copy(
              name = "  ",
              description = "  ",
              ingredients = emptyList(),
            ),
          )
        }

      response.status shouldBe HttpStatusCode.BadRequest
      val body = response.bodyAsText()
      body shouldContain "\"field\":\"name\""
      body shouldContain "\"field\":\"description\""
      body shouldContain "\"field\":\"ingredients\""
      coVerify(exactly = 0) { service.createRecipe(any()) }
    }

  // ---------------------------------------------------------------
  // PATCH /api/recipes/{id}
  // ---------------------------------------------------------------

  @Test
  fun `PATCH recipe with a valid payload calls editRecipe with the parsed body`() =
    testApplication {
      // KNOWN BUG: the route parses `payload` via call.receive(), validates it,
      // then calls editRecipe(uuid, call.receive()) -- a SECOND receive() on an
      // already-consumed request body. That's not a ContentTransformationException,
      // so it isn't caught by the dedicated 400 handler -- it falls through to the
      // catch-all Throwable handler and comes back as 500, not 200. This test
      // documents the intended behavior and will fail until editRecipe(uuid, payload)
      // reuses the already-parsed `payload` variable.
      val service = mockk<RecipeService>()
      val id = Uuid.random()
      val payload = EditRecipePayload(name = "New Name")
      coEvery { service.editRecipe(id, payload) } returns sampleRecipe(id)
      val client = setupTestApp(service)

      val response =
        client.patch("/api/recipes/$id") {
          contentType(ContentType.Application.Json)
          setBody(payload)
        }

      response.status shouldBe HttpStatusCode.OK
      coVerify(exactly = 1) { service.editRecipe(id, payload) }
    }

  @Test
  fun `PATCH recipe with a malformed uuid returns 400 and never calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/recipes/not-a-uuid") {
          contentType(ContentType.Application.Json)
          setBody(EditRecipePayload(name = "New Name"))
        }

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.editRecipe(any(), any()) }
    }

  @Test
  fun `PATCH recipe with a blank name (when present) returns a field error`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/recipes/${Uuid.random()}") {
          contentType(ContentType.Application.Json)
          setBody(EditRecipePayload(name = "   "))
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"name\""
    }

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
      val payload = EditIngredientPayload(name = "Flour", amount = "500g")
      coEvery { service.editIngredient(id, payload) } returns sampleIngredient(id)
      val client = setupTestApp(service)

      val response =
        client.patch("/api/ingredients/$id") {
          contentType(ContentType.Application.Json)
          setBody(payload)
        }

      response.status shouldBe HttpStatusCode.OK
      coVerify(exactly = 1) { service.editIngredient(id, payload) }
    }

  @Test
  fun `PATCH ingredient with a blank amount returns a field error and never calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/ingredients/${Uuid.random()}") {
          contentType(ContentType.Application.Json)
          setBody(EditIngredientPayload(name = "Flour", amount = "  "))
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"amount\""
      coVerify(exactly = 0) { service.editIngredient(any(), any()) }
    }

  @Test
  fun `PATCH ingredient with a blank name returns a field error`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/ingredients/${Uuid.random()}") {
          contentType(ContentType.Application.Json)
          setBody(EditIngredientPayload(name = "  ", amount = "500g"))
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
          setBody(EditIngredientPayload(name = "Flour", amount = "500g"))
        }

      response.status shouldBe HttpStatusCode.BadRequest
    }

  // ---------------------------------------------------------------
  // PATCH /api/instructions/{id}
  // ---------------------------------------------------------------

  @Test
  fun `PATCH instruction with a valid payload returns 200`() =
    testApplication {
      // Same double-receive() bug -- see note on the recipe PATCH test.
      val service = mockk<RecipeService>()
      val id = Uuid.random()
      val payload = EditInstructionPayload(description = "Mix, knead, and rest")
      coEvery { service.editInstruction(id, payload) } returns sampleInstruction(id)
      val client = setupTestApp(service)

      val response =
        client.patch("/api/instructions/$id") {
          contentType(ContentType.Application.Json)
          setBody(payload)
        }

      response.status shouldBe HttpStatusCode.OK
      coVerify(exactly = 1) { service.editInstruction(id, payload) }
    }

  @Test
  fun `PATCH instruction with a blank description returns a field error and never calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/instructions/${Uuid.random()}") {
          contentType(ContentType.Application.Json)
          setBody(EditInstructionPayload(description = "   "))
        }

      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "\"field\":\"description\""
      coVerify(exactly = 0) { service.editInstruction(any(), any()) }
    }

  // ---------------------------------------------------------------
  // PATCH /api/recipes/notes/{id}, /api/ingredients/notes/{id}, /api/instructions/notes/{id}
  // These routes have no validate{} block -- notes are optional free text,
  // so uuid parsing is the only real "business logic" to test. The routes
  // are also a bit unusual: updateRecipeNotes/updateIngredientNotes/
  // updateInstructionNotes return Unit, but the route still does
  // `val recipe = recipeService.updateRecipeNotes(...)` followed by
  // `call.respond(recipe)` -- so a successful request responds 200 with
  // an empty JSON body ("{}"), not the updated resource. Flagging this
  // as a likely design gap rather than fixing it here.
  // ---------------------------------------------------------------

  @Test
  fun `PATCH recipe notes with a valid body calls the service and returns 204`() =
    testApplication {
      val service = mockk<RecipeService>()
      val id = Uuid.random()
      coEvery { service.updateRecipeNotes(id, "Great bake!") } returns Unit
      val client = setupTestApp(service)

      val response =
        client.patch("/api/recipes/notes/$id") {
          contentType(ContentType.Application.Json)
          setBody("Great bake!")
        }

      response.status shouldBe HttpStatusCode.NoContent
      coVerify(exactly = 1) { service.updateRecipeNotes(id, "Great bake!") }
    }

  @Disabled("Temporarily skipped: Determining how to handle deleting notes")
  @Test
  fun `PATCH recipe notes with a null body clears the notes`() =
    testApplication {
      val service = mockk<RecipeService>()
      val id = Uuid.random()
      val notesSlot = slot<String?>()
      coEvery { service.updateRecipeNotes(id, captureNullable(notesSlot)) } returns Unit
      val client = setupTestApp(service)

      val response =
        client.patch("/api/recipes/notes/$id") {
          contentType(ContentType.Application.Json)
          setBody<String?>(null)
        }

      response.status shouldBe HttpStatusCode.NoContent
      notesSlot.captured shouldBe null
    }

  @Test
  fun `PATCH recipe notes with a malformed uuid returns 400 and never calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/recipes/notes/not-a-uuid") {
          contentType(ContentType.Application.Json)
          setBody("Great bake!")
        }

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.updateRecipeNotes(any(), any()) }
    }

  @Test
  fun `PATCH ingredient notes with a valid body calls the service and returns 204`() =
    testApplication {
      val service = mockk<RecipeService>()
      val id = Uuid.random()
      coEvery { service.updateIngredientNotes(id, "Reduce to 480g next time") } returns Unit
      val client = setupTestApp(service)

      val response =
        client.patch("/api/ingredients/notes/$id") {
          contentType(ContentType.Application.Json)
          setBody("Reduce to 480g next time")
        }

      response.status shouldBe HttpStatusCode.NoContent
      coVerify(exactly = 1) { service.updateIngredientNotes(id, "Reduce to 480g next time") }
    }

  @Test
  fun `PATCH ingredient notes with a malformed uuid returns 400 and never calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/ingredients/notes/not-a-uuid") {
          contentType(ContentType.Application.Json)
          setBody("note")
        }

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.updateIngredientNotes(any(), any()) }
    }

  @Test
  fun `PATCH instruction notes with a valid body calls the service and returns 204`() =
    testApplication {
      val service = mockk<RecipeService>()
      val id = Uuid.random()
      coEvery { service.updateInstructionNotes(id, "Rest longer in winter") } returns Unit
      val client = setupTestApp(service)

      val response =
        client.patch("/api/instructions/notes/$id") {
          contentType(ContentType.Application.Json)
          setBody("Rest longer in winter")
        }

      response.status shouldBe HttpStatusCode.NoContent
      coVerify(exactly = 1) { service.updateInstructionNotes(id, "Rest longer in winter") }
    }

  @Test
  fun `PATCH instruction notes with a malformed uuid returns 400 and never calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response =
        client.patch("/api/instructions/notes/not-a-uuid") {
          contentType(ContentType.Application.Json)
          setBody("note")
        }

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.updateInstructionNotes(any(), any()) }
    }

  // ---------------------------------------------------------------
  // DELETE /api/recipes/{id}
  // ---------------------------------------------------------------

  @Test
  fun `DELETE recipe with a valid id returns 204 and calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val id = Uuid.random()
      coEvery { service.deleteRecipe(id) } returns Unit
      val client = setupTestApp(service)

      val response = client.delete("/api/recipes/$id")

      response.status shouldBe HttpStatusCode.NoContent
      coVerify(exactly = 1) { service.deleteRecipe(id) }
    }

  @Test
  fun `DELETE recipe with a malformed uuid returns 400 and never calls the service`() =
    testApplication {
      val service = mockk<RecipeService>()
      val client = setupTestApp(service)

      val response = client.delete("/api/recipes/not-a-uuid")

      response.status shouldBe HttpStatusCode.BadRequest
      coVerify(exactly = 0) { service.deleteRecipe(any()) }
    }
}
