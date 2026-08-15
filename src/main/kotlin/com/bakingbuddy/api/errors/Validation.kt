package com.bakingbuddy.api.errors

import com.bakingbuddy.api.errors.FieldError
import com.bakingbuddy.api.errors.ValidationException

// =====================================================================
// 3. VALIDATION HELPER
// Collects all field errors instead of throwing on the first bad field.
// Call explicitly at the top of route handlers, before hitting the DB.
// =====================================================================

class Validator {
    private val errors = mutableListOf<FieldError>()

    fun require(condition: Boolean, field: String, message: String) {
        if (!condition) errors.add(FieldError(field, message))
    }

    fun requireNotBlank(value: String?, field: String) {
        require(!value.isNullOrBlank(), field, "must not be blank")
    }

    fun requirePositive(value: Number?, field: String) {
        if (value != null) require(value.toDouble() > 0, field, "must be positive")
    }

    fun finish() {
        if (errors.isNotEmpty()) throw ValidationException(errors.toList())
    }
}

fun validate(block: Validator.() -> Unit) {
    Validator().apply(block).finish()
}

// Example usage inside a route handler:
//
// val payload = call.receive<CreateRecipePayload>()
// validate {
//     requireNotBlank(payload.name, "name")
//     requireNotBlank(payload.recipeSource, "source")
// }