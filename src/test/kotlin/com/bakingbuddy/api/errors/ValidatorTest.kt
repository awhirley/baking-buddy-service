package com.bakingbuddy.api.errors

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid

/**
 * NOTE: assumes `FieldError(field: String, message: String)` based on how
 * Validator constructs it (`FieldError(field, message)`). Adjust the
 * shouldContainExactly calls if the real property order/names differ.
 */
class ValidatorTest {
    // -----------------------------------------------------------
    // Validator / validate{}
    // -----------------------------------------------------------

    @Test
    fun `validate does not throw when every check passes`() {
        validate {
            require(true, "field", "should not fail")
            requireNotBlank("value", "field2")
            requireNotBlankIfPresent(null, "field3")
            requirePositive(10, "field4")
        }
        // reaching here without an exception is the assertion
    }

    @Test
    fun `validate throws ValidationException carrying the failing field`() {
        val exception =
            assertFailsWith<ValidationException> {
                validate {
                    require(false, "name", "must not be blank")
                }
            }

        exception.fieldErrors shouldContainExactly listOf(FieldError("name", "must not be blank"))
    }

    @Test
    fun `validate collects every failing check, not just the first`() {
        val exception =
            assertFailsWith<ValidationException> {
                validate {
                    require(false, "name", "must not be blank")
                    require(true, "description", "should not fail")
                    require(false, "ingredients", "must contain at least one ingredient")
                }
            }

        exception.fieldErrors shouldContainExactly
            listOf(
                FieldError("name", "must not be blank"),
                FieldError("ingredients", "must contain at least one ingredient"),
            )
    }

    @Test
    fun `validate preserves the order checks were declared in`() {
        val exception =
            assertFailsWith<ValidationException> {
                validate {
                    require(false, "third", "fails")
                    require(false, "second", "fails")
                    require(false, "first", "fails")
                }
            }

        exception.fieldErrors.map { it.field } shouldBe listOf("third", "second", "first")
    }

    @Test
    fun `requireNotBlank fails on null`() {
        val exception =
            assertFailsWith<ValidationException> {
                validate { requireNotBlank(null, "name") }
            }
        exception.fieldErrors shouldContainExactly listOf(FieldError("name", "must not be blank"))
    }

    @Test
    fun `requireNotBlank fails on an empty string`() {
        assertFailsWith<ValidationException> {
            validate { requireNotBlank("", "name") }
        }
    }

    @Test
    fun `requireNotBlank fails on a whitespace-only string`() {
        assertFailsWith<ValidationException> {
            validate { requireNotBlank("   ", "name") }
        }
    }

    @Test
    fun `requireNotBlank passes on a non-blank string`() {
        validate { requireNotBlank("Sourdough", "name") }
    }

    @Test
    fun `requireNotBlankIfPresent does not fail when the value is null`() {
        validate { requireNotBlankIfPresent(null, "recipeSource") }
    }

    @Test
    fun `requireNotBlankIfPresent fails when the value is present but blank`() {
        val exception =
            assertFailsWith<ValidationException> {
                validate { requireNotBlankIfPresent("   ", "recipeSource") }
            }
        exception.fieldErrors shouldContainExactly listOf(FieldError("recipeSource", "must not be blank"))
    }

    @Test
    fun `requireNotBlankIfPresent passes when the value is present and non-blank`() {
        validate { requireNotBlankIfPresent("Grandma's recipe", "recipeSource") }
    }

    @Test
    fun `requirePositive does not fail when the value is null`() {
        validate { requirePositive(null, "elevation") }
    }

    @Test
    fun `requirePositive fails on zero`() {
        val exception =
            assertFailsWith<ValidationException> {
                validate { requirePositive(0, "elevation") }
            }
        exception.fieldErrors shouldContainExactly listOf(FieldError("elevation", "must be positive"))
    }

    @Test
    fun `requirePositive fails on a negative number`() {
        assertFailsWith<ValidationException> {
            validate { requirePositive(-5, "elevation") }
        }
    }

    @Test
    fun `requirePositive passes on a positive number`() {
        validate { requirePositive(1500, "elevation") }
    }

    // -----------------------------------------------------------
    // ApplicationCall.requireUuidParam
    //
    // This needs an ApplicationCall, so it's exercised through a bare
    // Ktor test route rather than called directly.
    // -----------------------------------------------------------

    @Test
    fun `requireUuidParam parses a valid uuid`() =
        testApplication {
            application {
                install(StatusPages) {
                    exception<ApiException> { call, cause -> call.respond(cause.statusCode, cause.message) }
                }
                routing {
                    get("/test/{id}") {
                        val uuid = call.requireUuidParam("id")
                        call.respond(uuid.toString())
                    }
                }
            }

            val id = Uuid.random()
            val response = client.get("/test/$id")

            response.status shouldBe HttpStatusCode.OK
        }

    @Test
    fun `requireUuidParam returns 400 for a malformed uuid`() =
        testApplication {
            application {
                install(StatusPages) {
                    exception<ApiException> { call, cause -> call.respond(cause.statusCode, cause.message) }
                }
                routing {
                    get("/test/{id}") {
                        val uuid = call.requireUuidParam("id")
                        call.respond(uuid.toString())
                    }
                }
            }

            val response = client.get("/test/not-a-uuid")

            response.status shouldBe HttpStatusCode.BadRequest
        }
}
