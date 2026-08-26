package com.bakingbuddy.repositories

import com.bakingbuddy.api.errors.DataIntegrityException
import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.database.BakeIngredientsTable
import com.bakingbuddy.database.BakeInstructionsTable
import com.bakingbuddy.database.BakesTable
import com.bakingbuddy.database.IngredientDeltaTable
import com.bakingbuddy.database.IngredientsTable
import com.bakingbuddy.database.InstructionDeltaTable
import com.bakingbuddy.database.InstructionsTable
import com.bakingbuddy.database.RecipesTable
import com.bakingbuddy.models.bakes.Bake
import com.bakingbuddy.models.bakes.BakeDetail
import com.bakingbuddy.models.bakes.BakeIngredientPayload
import com.bakingbuddy.models.bakes.BakeInstructionPayload
import com.bakingbuddy.models.bakes.UpdateBakePayload
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import kotlin.uuid.Uuid

class BakeRepositoryImpl : BakeRepository {
  private fun getBestIngredientDeltas(recipeId: Uuid): List<Triple<Uuid, Uuid, Int>> {
    val ingredientDeltas =
      IngredientsTable
        .join(
          IngredientDeltaTable,
          JoinType.INNER,
          onColumn = IngredientsTable.id,
          otherColumn = IngredientDeltaTable.ingredient_id,
          additionalConstraint = { IngredientDeltaTable.version eq IngredientsTable.best_version },
        ).selectAll()
        .where { IngredientsTable.recipe_id eq recipeId }
        .map { row ->
          Triple(
            row[IngredientDeltaTable.id],
            row[IngredientDeltaTable.ingredient_id],
            row[IngredientDeltaTable.version],
          )
        }

    val ingredientConceptCount =
      IngredientsTable
        .selectAll()
        .where { IngredientsTable.recipe_id eq recipeId }
        .count()

    if (ingredientDeltas.size.toLong() != ingredientConceptCount) {
      throw DataIntegrityException(
        "Missing ingredient_delta row for best_version on one or more ingredients of recipe $recipeId",
      )
    }

    return ingredientDeltas
  }

  private fun getBestInstructionDeltas(recipeId: Uuid): List<Triple<Uuid, Uuid, Int>> {
    val instructionDeltas =
      InstructionsTable
        .join(
          InstructionDeltaTable,
          JoinType.INNER,
          onColumn = InstructionsTable.id,
          otherColumn = InstructionDeltaTable.instruction_id,
          additionalConstraint = { InstructionDeltaTable.version eq InstructionsTable.best_version },
        ).selectAll()
        .where { InstructionsTable.recipe_id eq recipeId }
        .map { row ->
          Triple(
            row[InstructionDeltaTable.id],
            row[InstructionDeltaTable.instruction_id],
            row[InstructionDeltaTable.version],
          )
        }

    val instructionConceptCount =
      InstructionsTable
        .selectAll()
        .where { InstructionsTable.recipe_id eq recipeId }
        .count()

    if (instructionDeltas.size.toLong() != instructionConceptCount) {
      throw DataIntegrityException(
        "Missing instruction_delta row for best_version on one or more instructions of recipe $recipeId",
      )
    }

    return instructionDeltas
  }

  override suspend fun createBake(recipeId: Uuid): Bake =
    transaction {
      RecipesTable
        .selectAll()
        .where { RecipesTable.id eq recipeId }
        .singleOrNull() ?: throw NotFoundException("Recipe", recipeId.toString())

      // Pull the current best_version delta for every ingredient/instruction of this recipe.
      val ingredientDeltas = getBestIngredientDeltas(recipeId)
      val instructionDeltas = getBestInstructionDeltas(recipeId)

      val bakeId = Uuid.random()
      val createdAt = Instant.now()

      BakesTable.insert {
        it[BakesTable.id] = bakeId
        it[BakesTable.recipe_id] = recipeId
        it[BakesTable.created_at] = createdAt
        it[BakesTable.start_datetime] = createdAt
      }

      ingredientDeltas.forEach { (deltaId, _, _) ->
        BakeIngredientsTable.insert {
          it[BakeIngredientsTable.id] = Uuid.random()
          it[BakeIngredientsTable.bake_id] = bakeId
          it[BakeIngredientsTable.ingredient_delta_id] = deltaId
        }
      }

      instructionDeltas.forEach { (deltaId, _, _) ->
        BakeInstructionsTable.insert {
          it[BakeInstructionsTable.id] = Uuid.random()
          it[BakeInstructionsTable.bake_id] = bakeId
          it[BakeInstructionsTable.instruction_delta_id] = deltaId
        }
      }

      val bakeDetail =
        BakeDetail(
          id = bakeId,
          recipeId = recipeId,
          createdAt = createdAt,
          startDatetime = createdAt,
        )

      Bake(
        id = bakeId,
        recipeId = recipeId,
        details = bakeDetail,
        ingredientVersions =
          ingredientDeltas.map { (_, ingredientId, version) ->
            BakeIngredientPayload(ingredientId = ingredientId, version = version)
          },
        instructionVersions =
          instructionDeltas.map { (_, instructionId, version) ->
            BakeInstructionPayload(instructionId = instructionId, version = version)
          },
      )
    }

  // Load all bakes with ingredients and instructions
  override suspend fun listBakesWithProcedure(recipeId: Uuid): List<Bake> {
    return transaction {
      val bakeRows =
        BakesTable
          .selectAll()
          .where { BakesTable.recipe_id eq recipeId }
          .toList()

      if (bakeRows.isEmpty()) return@transaction emptyList()

      val bakeIds = bakeRows.map { it[BakesTable.id] }

      val ingredientVersionsByBake =
        BakeIngredientsTable
          .join(
            IngredientDeltaTable,
            JoinType.INNER,
            onColumn = BakeIngredientsTable.ingredient_delta_id,
            otherColumn = IngredientDeltaTable.id,
          ).selectAll()
          .where { BakeIngredientsTable.bake_id inList bakeIds }
          .map { row ->
            row[BakeIngredientsTable.bake_id] to
              BakeIngredientPayload(
                ingredientId = row[IngredientDeltaTable.ingredient_id],
                version = row[IngredientDeltaTable.version],
              )
          }.groupBy({ it.first }, { it.second })

      val instructionVersionsByBake =
        BakeInstructionsTable
          .join(
            InstructionDeltaTable,
            JoinType.INNER,
            onColumn = BakeInstructionsTable.instruction_delta_id,
            otherColumn = InstructionDeltaTable.id,
          ).selectAll()
          .where { BakeInstructionsTable.bake_id inList bakeIds }
          .map { row ->
            row[BakeInstructionsTable.bake_id] to
              BakeInstructionPayload(
                instructionId = row[InstructionDeltaTable.instruction_id],
                version = row[InstructionDeltaTable.version],
              )
          }.groupBy({ it.first }, { it.second })

      bakeRows.map { row ->
        val bakeId = row[BakesTable.id]
        val bakeDetail =
          BakeDetail(
            id = bakeId,
            recipeId = row[BakesTable.recipe_id],
            elevation = row[BakesTable.elevation],
            notes = row[BakesTable.notes],
            createdAt = row[BakesTable.created_at],
            startDatetime = row[BakesTable.start_datetime],
            endDatetime = row[BakesTable.end_datetime],
          )
        Bake(
          id = bakeId,
          recipeId = row[BakesTable.recipe_id],
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
      val bakeRows =
        BakesTable
          .selectAll()
          .where { BakesTable.recipe_id eq recipeId }
          .toList()

      if (bakeRows.isEmpty()) return@transaction emptyList()

      val bakeIds = bakeRows.map { it[BakesTable.id] }

      bakeRows.map { row ->
        val bakeId = row[BakesTable.id]
        BakeDetail(
          id = bakeId,
          recipeId = row[BakesTable.recipe_id],
          elevation = row[BakesTable.elevation],
          notes = row[BakesTable.notes],
          createdAt = row[BakesTable.created_at],
          startDatetime = row[BakesTable.start_datetime],
          endDatetime = row[BakesTable.end_datetime],
        )
      }
    }
  }

  override suspend fun updateBake(payload: UpdateBakePayload): Unit =
    transaction {
      BakesTable
        .selectAll()
        .where { BakesTable.id eq payload.bakeId }
        .singleOrNull() ?: throw NotFoundException("Bake", payload.bakeId.toString())

      BakesTable.update({ BakesTable.id eq payload.bakeId }) {
        payload.startDatetime?.let { start -> it[BakesTable.start_datetime] = start }
        payload.endDatetime?.let { end -> it[BakesTable.end_datetime] = end }
        payload.elevation?.let { elevation -> it[BakesTable.elevation] = elevation }
        payload.notes?.let { notes -> it[BakesTable.notes] = notes }
      }
    }

  override suspend fun deleteBake(id: Uuid): Unit =
    transaction {
      val existing =
        BakesTable
          .selectAll()
          .where { BakesTable.id eq id }
          .singleOrNull() ?: throw NotFoundException("Bake", id.toString())

      BakeIngredientsTable.deleteWhere { BakeIngredientsTable.bake_id eq id }
      BakeInstructionsTable.deleteWhere { BakeInstructionsTable.bake_id eq id }
      BakesTable.deleteWhere { BakesTable.id eq id }
    }
}
