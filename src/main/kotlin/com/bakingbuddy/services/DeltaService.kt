package com.bakingbuddy.services

import com.bakingbuddy.models.IngredientDeltaEntry
import com.bakingbuddy.models.IngredientHistory
import com.bakingbuddy.models.InstructionDeltaEntry
import com.bakingbuddy.models.InstructionHistory
import com.bakingbuddy.repositories.DeltaRepositoryImpl
import kotlin.uuid.Uuid

class DeltaService {
    private val deltaRepository = DeltaRepositoryImpl()
    
    suspend fun getIngredientHistory(ingredientId: Uuid): IngredientHistory {
        return deltaRepository.getIngredientHistory(ingredientId)
    }
    
    suspend fun getInstructionHistory(instructionId: Uuid): InstructionHistory {
        return deltaRepository.getInstructionHistory(instructionId)
    }
}