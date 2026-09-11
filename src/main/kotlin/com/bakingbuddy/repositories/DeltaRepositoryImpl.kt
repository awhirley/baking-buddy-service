package com.bakingbuddy.repositories

import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.database.BakeIngredientsTable
import com.bakingbuddy.database.BakeInstructionsTable
import com.bakingbuddy.database.BakesTable
import com.bakingbuddy.database.IngredientDeltaTable
import com.bakingbuddy.database.IngredientsTable
import com.bakingbuddy.database.InstructionDeltaTable
import com.bakingbuddy.database.InstructionsTable
import com.bakingbuddy.models.bakes.BakeDetail
import com.bakingbuddy.models.ingredients.IngredientDeltaEntry
import com.bakingbuddy.models.ingredients.IngredientHistory
import com.bakingbuddy.models.instructions.InstructionDeltaEntry
import com.bakingbuddy.models.instructions.InstructionHistory
import com.bakingbuddy.repositories.helpers.getBakeRatings
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

class DeltaRepositoryImpl : DeltaRepository {
  override suspend fun getIngredientHistory(id: Uuid): IngredientHistory =
    transaction {
      val ingredientRow =
        IngredientsTable
          .selectAll()
          .where { IngredientsTable.id eq id }
          .singleOrNull() ?: throw NotFoundException("Ingredient", id.toString())

      val history =
        IngredientDeltaTable
          .selectAll()
          .where { IngredientDeltaTable.ingredient_id eq id }
          .orderBy(IngredientDeltaTable.version)
          .map { row ->
            IngredientDeltaEntry(
              id = row[IngredientDeltaTable.id],
              ingredientId = row[IngredientDeltaTable.ingredient_id],
              version = row[IngredientDeltaTable.version],
              name = row[IngredientDeltaTable.name],
              amount = row[IngredientDeltaTable.amount],
              notes = row[IngredientDeltaTable.notes],
              createdAt = row[IngredientDeltaTable.created_at],
              order = row[IngredientDeltaTable.order],
            )
          }

      IngredientHistory(
        id = ingredientRow[IngredientsTable.id],
        recipeId = ingredientRow[IngredientsTable.recipe_id],
        bestVersion = ingredientRow[IngredientsTable.best_version],
        history = history,
      )
    }

  override suspend fun getInstructionHistory(id: Uuid): InstructionHistory =
    transaction {
      val instructionRow =
        InstructionsTable
          .selectAll()
          .where { InstructionsTable.id eq id }
          .singleOrNull() ?: throw NotFoundException("Instruction", id.toString())

      val history =
        InstructionDeltaTable
          .selectAll()
          .where { InstructionDeltaTable.instruction_id eq id }
          .orderBy(InstructionDeltaTable.version)
          .map { row ->
            InstructionDeltaEntry(
              id = row[InstructionDeltaTable.id],
              instructionId = row[InstructionDeltaTable.instruction_id],
              version = row[InstructionDeltaTable.version],
              description = row[InstructionDeltaTable.description],
              notes = row[InstructionDeltaTable.notes],
              createdAt = row[InstructionDeltaTable.created_at],
              order = row[InstructionDeltaTable.order],
            )
          }

      InstructionHistory(
        id = instructionRow[InstructionsTable.id],
        recipeId = instructionRow[InstructionsTable.recipe_id],
        bestVersion = instructionRow[InstructionsTable.best_version],
        history = history,
      )
    }

  override suspend fun getBakesByIngredientDeltaId(ingredientDeltaId: Uuid): List<BakeDetail> =
    transaction {
      val bakeRows =
        BakesTable
          .innerJoin(BakeIngredientsTable, { BakesTable.id }, { BakeIngredientsTable.bake_id })
          .selectAll()
          .where {
            (BakeIngredientsTable.ingredient_delta_id eq ingredientDeltaId) or
              (BakeIngredientsTable.completed_bake_delta_id eq ingredientDeltaId)
          }.distinctBy { it[BakesTable.id] }

      val bakeIds = bakeRows.map { it[BakesTable.id] }
      val ratingsByBakeId = getBakeRatings(bakeIds)

      bakeRows.map { row ->
        BakeDetail(
          id = row[BakesTable.id],
          recipeId = row[BakesTable.recipe_id],
          elevation = row[BakesTable.elevation],
          notes = row[BakesTable.notes],
          createdAt = row[BakesTable.created_at],
          startDatetime = row[BakesTable.start_datetime],
          endDatetime = row[BakesTable.end_datetime],
          ratings = ratingsByBakeId[row[BakesTable.id]],
        )
      }
    }

  override suspend fun getBakesByInstructionDeltaId(instructionDeltaId: Uuid): List<BakeDetail> =
    transaction {
      val bakeRows =
        BakesTable
          .innerJoin(BakeInstructionsTable, { BakesTable.id }, { BakeInstructionsTable.bake_id })
          .selectAll()
          .where {
            (BakeInstructionsTable.instruction_delta_id eq instructionDeltaId) or
              (BakeInstructionsTable.completed_bake_delta_id eq instructionDeltaId)
          }.distinctBy { it[BakesTable.id] }

      val bakeIds = bakeRows.map { it[BakesTable.id] }
      val ratingsByBakeId = getBakeRatings(bakeIds)

      bakeRows.map { row ->
        BakeDetail(
          id = row[BakesTable.id],
          recipeId = row[BakesTable.recipe_id],
          elevation = row[BakesTable.elevation],
          notes = row[BakesTable.notes],
          createdAt = row[BakesTable.created_at],
          startDatetime = row[BakesTable.start_datetime],
          endDatetime = row[BakesTable.end_datetime],
          ratings = ratingsByBakeId[row[BakesTable.id]],
        )
      }
    }
}
