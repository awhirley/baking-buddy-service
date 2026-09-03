package com.bakingbuddy.repositories

import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.database.IngredientDeltaTable
import com.bakingbuddy.database.IngredientsTable
import com.bakingbuddy.database.InstructionDeltaTable
import com.bakingbuddy.database.InstructionsTable
import com.bakingbuddy.models.ingredients.IngredientDeltaEntry
import com.bakingbuddy.models.ingredients.IngredientHistory
import com.bakingbuddy.models.instructions.InstructionDeltaEntry
import com.bakingbuddy.models.instructions.InstructionHistory
import org.jetbrains.exposed.v1.core.eq
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
}
