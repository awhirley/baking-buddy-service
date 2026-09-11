package com.bakingbuddy.repositories

import com.bakingbuddy.models.bakes.BakeDetail
import com.bakingbuddy.models.ingredients.IngredientHistory
import com.bakingbuddy.models.instructions.InstructionHistory
import kotlin.uuid.Uuid

interface DeltaRepository {
  suspend fun getIngredientHistory(id: Uuid): IngredientHistory

  suspend fun getInstructionHistory(id: Uuid): InstructionHistory

  suspend fun getBakesByIngredientDeltaId(ingredientDeltaId: Uuid): List<BakeDetail>

  suspend fun getBakesByInstructionDeltaId(instructionDeltaId: Uuid): List<BakeDetail>
}
