package com.bakingbuddy.repositories

import com.bakingbuddy.models.bakeStorage.BakeImageResponse
import com.bakingbuddy.models.ingredients.IngredientHistory
import com.bakingbuddy.models.instructions.InstructionHistory
import kotlin.uuid.Uuid

interface BakeStorageRepository {
  suspend fun uploadImageToBake(id: Uuid, path: String, imageUrl: String): Unit
}
