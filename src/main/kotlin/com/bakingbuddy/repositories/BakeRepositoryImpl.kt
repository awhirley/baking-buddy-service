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
import com.bakingbuddy.models.bakes.BakeDetail
import com.bakingbuddy.models.bakes.BakeIngredientPayload
import com.bakingbuddy.models.bakes.BakeInstructionPayload
import com.bakingbuddy.models.bakes.UpdateBakePayload
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import kotlin.uuid.Uuid

class BakeRepositoryImpl : BakeRepository {
  override suspend fun createBake(recipeId: Uuid): Bake {
    return transaction {
        Recipes
            .selectAll()
            .where { Recipes.id eq recipeId }
            .singleOrNull() ?: throw NoSuchElementException("Recipe $recipeId not found")

        // Pull the current best_version delta for every ingredient/instruction of this recipe.
        val ingredientDeltas = Ingredients
            .join(
                IngredientDelta,
                JoinType.INNER,
                onColumn = Ingredients.id,
                otherColumn = IngredientDelta.ingredient_id,
                additionalConstraint = { IngredientDelta.version eq Ingredients.best_version },
            )
            .selectAll()
            .where { Ingredients.recipe_id eq recipeId }
            .map { row ->
                Triple(
                    row[IngredientDelta.id],
                    row[IngredientDelta.ingredient_id],
                    row[IngredientDelta.version],
                )
            }

        val ingredientConceptCount = Ingredients
            .selectAll()
            .where { Ingredients.recipe_id eq recipeId }
            .count()
        check(ingredientDeltas.size.toLong() == ingredientConceptCount) {
            "Missing ingredient_delta row for best_version on one or more ingredients of recipe $recipeId"
        }

        val instructionDeltas = Instructions
            .join(
                InstructionDelta,
                JoinType.INNER,
                onColumn = Instructions.id,
                otherColumn = InstructionDelta.instruction_id,
                additionalConstraint = { InstructionDelta.version eq Instructions.best_version },
            )
            .selectAll()
            .where { Instructions.recipe_id eq recipeId }
            .map { row ->
                Triple(
                    row[InstructionDelta.id],
                    row[InstructionDelta.instruction_id],
                    row[InstructionDelta.version],
                )
            }

        val instructionConceptCount = Instructions
            .selectAll()
            .where { Instructions.recipe_id eq recipeId }
            .count()
        check(instructionDeltas.size.toLong() == instructionConceptCount) {
            "Missing instruction_delta row for best_version on one or more instructions of recipe $recipeId"
        }

        val bakeId = Uuid.random()
        val createdAt = Instant.now()

        Bakes.insert {
            it[Bakes.id] = bakeId
            it[Bakes.recipe_id] = recipeId
            it[Bakes.created_at] = createdAt
        }

        ingredientDeltas.forEach { (deltaId, _, _) ->
            BakeIngredients.insert {
                it[BakeIngredients.id] = Uuid.random()
                it[BakeIngredients.bake_id] = bakeId
                it[BakeIngredients.ingredient_delta_id] = deltaId
            }
        }

        instructionDeltas.forEach { (deltaId, _, _) ->
            BakeInstructions.insert {
                it[BakeInstructions.id] = Uuid.random()
                it[BakeInstructions.bake_id] = bakeId
                it[BakeInstructions.instruction_delta_id] = deltaId
            }
        }

        val bakeDetail = BakeDetail(
            id = bakeId,
            recipeId = recipeId,
            createdAt = createdAt,
        )

        Bake(
            id = bakeId,
            recipeId = recipeId,
            details = bakeDetail,
            ingredientVersions = ingredientDeltas.map { (_, ingredientId, version) ->
                BakeIngredientPayload(ingredientId = ingredientId, version = version)
            },
            instructionVersions = instructionDeltas.map { (_, instructionId, version) ->
                BakeInstructionPayload(instructionId = instructionId, version = version)
            },
        )
    }
}

	// Load all bakes with ingredients and instructions
  override suspend fun listBakesWithProcedure(recipeId: Uuid): List<Bake> {
    return transaction {
        val bakeRows = Bakes
            .selectAll()
            .where { Bakes.recipe_id eq recipeId }
            .toList()

        if (bakeRows.isEmpty()) return@transaction emptyList()

        val bakeIds = bakeRows.map { it[Bakes.id] }

        val ingredientVersionsByBake = BakeIngredients
            .join(
                IngredientDelta,
                JoinType.INNER,
                onColumn = BakeIngredients.ingredient_delta_id,
                otherColumn = IngredientDelta.id,
            )
            .selectAll()
            .where { BakeIngredients.bake_id inList bakeIds }
            .map { row ->
                row[BakeIngredients.bake_id] to BakeIngredientPayload(
                    ingredientId = row[IngredientDelta.ingredient_id],
                    version = row[IngredientDelta.version],
                )
            }
            .groupBy({ it.first }, { it.second })

        val instructionVersionsByBake = BakeInstructions
            .join(
                InstructionDelta,
                JoinType.INNER,
                onColumn = BakeInstructions.instruction_delta_id,
                otherColumn = InstructionDelta.id,
            )
            .selectAll()
            .where { BakeInstructions.bake_id inList bakeIds }
            .map { row ->
                row[BakeInstructions.bake_id] to BakeInstructionPayload(
                    instructionId = row[InstructionDelta.instruction_id],
                    version = row[InstructionDelta.version],
                )
            }
            .groupBy({ it.first }, { it.second })

        bakeRows.map { row ->
            val bakeId = row[Bakes.id]
            val bakeDetail = BakeDetail(
            	id = bakeId,
            	recipeId = row[Bakes.recipe_id],
                date = row[Bakes.date],
                results = row[Bakes.results],
                elevation = row[Bakes.elevation],
                notes = row[Bakes.notes],
                createdAt = row[Bakes.created_at],
            )
            Bake(
                id = bakeId,
                recipeId = row[Bakes.recipe_id],
                details = bakeDetail,
                ingredientVersions = ingredientVersionsByBake[bakeId] ?: emptyList(),
              instructionVersions = instructionVersionsByBake[bakeId] ?: emptyList(),
            )
        }
    }
  }

  // Load all details of all bakes
	override suspend fun listBakes(recipeId: Uuid): List<BakeDetail> {
    return transaction {
        val bakeRows = Bakes
            .selectAll()
            .where { Bakes.recipe_id eq recipeId }
            .toList()

        if (bakeRows.isEmpty()) return@transaction emptyList()

        val bakeIds = bakeRows.map { it[Bakes.id] }

        bakeRows.map { row ->
            val bakeId = row[Bakes.id]
            BakeDetail(
            	id = bakeId,
            	recipeId = row[Bakes.recipe_id],
							date = row[Bakes.date],
							results = row[Bakes.results],
							elevation = row[Bakes.elevation],
							notes = row[Bakes.notes],
							createdAt = row[Bakes.created_at],
            )
        }
    }
  }

  // override suspend fun updateBake(payload: UpdateBakePayload): Bake {
    
  // }  
}