package com.bakingbuddy.repositories

import com.bakingbuddy.database.IngredientDelta
import com.bakingbuddy.database.Ingredients
import com.bakingbuddy.database.InstructionDelta
import com.bakingbuddy.database.Instructions
import com.bakingbuddy.models.IngredientDeltaEntry
import com.bakingbuddy.models.IngredientHistory
import com.bakingbuddy.models.InstructionDeltaEntry
import com.bakingbuddy.models.InstructionHistory
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import kotlin.uuid.Uuid

class DeltaRepositoryImpl : DeltaRepository {
  override suspend fun getIngredientHistory(id: Uuid): IngredientHistory {
    return transaction {
      val ingredientRow = Ingredients
          .selectAll()
          .where { Ingredients.id eq id }
          .singleOrNull() ?: throw NoSuchElementException("Ingredient $id not found")

      val history = IngredientDelta
          .selectAll()
          .where { IngredientDelta.ingredient_id eq id }
          .orderBy(IngredientDelta.version)
          .map { row ->
              IngredientDeltaEntry(
                id = row[IngredientDelta.id],
                ingredientId = row[IngredientDelta.ingredient_id],
                version = row[IngredientDelta.version],
                name = row[IngredientDelta.name],
                amount = row[IngredientDelta.amount],
                createdAt = row[IngredientDelta.created_at],
              )
          }

      IngredientHistory(
          id = ingredientRow[Ingredients.id],
          recipeId = ingredientRow[Ingredients.recipe_id],
          bestVersion = ingredientRow[Ingredients.best_version],
          history = history,
      )
    }
  }
      
  override suspend fun getInstructionHistory(id: Uuid): InstructionHistory {
    return transaction {
        val instructionRow = Instructions
            .selectAll()
            .where { Instructions.id eq id }
            .singleOrNull() ?: throw NoSuchElementException("Instruction $id not found")

        val history = InstructionDelta
            .selectAll()
            .where { InstructionDelta.instruction_id eq id }
            .orderBy(InstructionDelta.version)
            .map { row ->
                InstructionDeltaEntry(
                  id = row[InstructionDelta.id],
                  instructionId = row[InstructionDelta.instruction_id],
                  version = row[InstructionDelta.version],
                  description = row[InstructionDelta.description],
                  createdAt = row[InstructionDelta.created_at],
                )
            }

        InstructionHistory(
            id = instructionRow[Instructions.id],
            recipeId = instructionRow[Instructions.recipe_id],
            bestVersion = instructionRow[Instructions.best_version],
            history = history,
        )
    }
  }
}