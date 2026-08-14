package com.bakingbuddy.repositories

import com.bakingbuddy.models.ingredients.IngredientHistory
import com.bakingbuddy.models.instructions.InstructionHistory
import kotlin.uuid.Uuid

interface DeltaRepository {
    suspend fun getIngredientHistory(id: Uuid): IngredientHistory
      
    suspend fun getInstructionHistory(id: Uuid): InstructionHistory
}