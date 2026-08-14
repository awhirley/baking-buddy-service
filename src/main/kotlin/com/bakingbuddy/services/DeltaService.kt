package com.bakingbuddy.services

import com.bakingbuddy.models.ingredients.IngredientHistory
import com.bakingbuddy.models.instructions.InstructionHistory
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