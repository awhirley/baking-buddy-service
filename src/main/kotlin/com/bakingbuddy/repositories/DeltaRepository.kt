package com.bakingbuddy.repositories

import com.bakingbuddy.models.IngredientDeltaEntry
import com.bakingbuddy.models.IngredientHistory
import com.bakingbuddy.models.InstructionDeltaEntry
import com.bakingbuddy.models.InstructionHistory
import kotlin.uuid.Uuid

interface DeltaRepository {
    suspend fun getIngredientHistory(id: Uuid): IngredientHistory
      
    suspend fun getInstructionHistory(id: Uuid): InstructionHistory
}