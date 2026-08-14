package com.bakingbuddy.repositories

import com.bakingbuddy.database.BakeIngredients
import com.bakingbuddy.database.BakeInstructions
import com.bakingbuddy.database.Bakes
import com.bakingbuddy.database.IngredientDelta
import com.bakingbuddy.database.Ingredients
import com.bakingbuddy.database.InstructionDelta
import com.bakingbuddy.database.Instructions
import com.bakingbuddy.database.Recipes
import com.bakingbuddy.models.bakes.Bake
import com.bakingbuddy.models.bakes.CreateBakePayload
import com.bakingbuddy.models.ingredients.IngredientDeltaEntry
import com.bakingbuddy.models.ingredients.IngredientHistory
import com.bakingbuddy.models.instructions.InstructionDeltaEntry
import com.bakingbuddy.models.instructions.InstructionHistory
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import kotlin.uuid.Uuid

class BakeRepositoryImpl : BakeRepository {
  override suspend fun createBake(payload: CreateBakePayload): Bake {
    return transaction {
        Recipes
            .selectAll()
            .where { Recipes.id eq payload.recipeId }
            .singleOrNull() ?: throw NoSuchElementException("Recipe ${payload.recipeId} not found")

        val currentIngredientIds = Ingredients
            .selectAll()
            .where { Ingredients.recipe_id eq payload.recipeId }
            .map { it[Ingredients.id] }
            .toSet()
        val providedIngredientIds = payload.ingredientVersions.map { it.ingredientId }.toSet()
        require(providedIngredientIds == currentIngredientIds) {
            "Bake must specify exactly one version for every current ingredient of recipe ${payload.recipeId}. " +
                "Missing: ${currentIngredientIds - providedIngredientIds}, " +
                "Unexpected: ${providedIngredientIds - currentIngredientIds}"
        }

        val currentInstructionIds = Instructions
            .selectAll()
            .where { Instructions.recipe_id eq payload.recipeId }
            .map { it[Instructions.id] }
            .toSet()
        val providedInstructionIds = payload.instructionVersions.map { it.instructionId }.toSet()
        require(providedInstructionIds == currentInstructionIds) {
            "Bake must specify exactly one version for every current instruction of recipe ${payload.recipeId}. " +
                "Missing: ${currentInstructionIds - providedInstructionIds}, " +
                "Unexpected: ${providedInstructionIds - currentInstructionIds}"
        }

        val ingredientDeltaIds = payload.ingredientVersions.map { pin ->
            IngredientDelta
                .selectAll()
                .where { (IngredientDelta.ingredient_id eq pin.ingredientId) and (IngredientDelta.version eq pin.version) }
                .singleOrNull()?.get(IngredientDelta.id)
                ?: throw NoSuchElementException("No delta found for ingredient ${pin.ingredientId} version ${pin.version}")
        }

        val instructionDeltaIds = payload.instructionVersions.map { pin ->
            InstructionDelta
                .selectAll()
                .where { (InstructionDelta.instruction_id eq pin.instructionId) and (InstructionDelta.version eq pin.version) }
                .singleOrNull()?.get(InstructionDelta.id)
                ?: throw NoSuchElementException("No delta found for instruction ${pin.instructionId} version ${pin.version}")
        }

        val bakeId = Uuid.random()
        val createdAt = Instant.now()

        Bakes.insert {
            it[Bakes.id] = bakeId
            it[Bakes.recipe_id] = payload.recipeId
            it[Bakes.date] = payload.date
            it[Bakes.results] = payload.results
            it[Bakes.elevation] = payload.elevation
            it[Bakes.notes] = payload.notes
            it[Bakes.created_at] = createdAt
        }

        ingredientDeltaIds.forEach { deltaId ->
            BakeIngredients.insert {
                it[BakeIngredients.id] = Uuid.random()
                it[BakeIngredients.bake_id] = bakeId
                it[BakeIngredients.ingredient_delta_id] = deltaId
            }
        }

        instructionDeltaIds.forEach { deltaId ->
            BakeInstructions.insert {
                it[BakeInstructions.id] = Uuid.random()
                it[BakeInstructions.bake_id] = bakeId
                it[BakeInstructions.instruction_delta_id] = deltaId
            }
        }

        Bake(
            id = bakeId,
            recipeId = payload.recipeId,
            date = payload.date,
            results = payload.results,
            elevation = payload.elevation,
            notes = payload.notes,
            createdAt = createdAt,
            ingredientVersions = payload.ingredientVersions,
            instructionVersions = payload.instructionVersions,
        )
    }
}
}