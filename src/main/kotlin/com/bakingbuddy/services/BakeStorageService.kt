package com.bakingbuddy.services

import com.bakingbuddy.models.bakeStorage.BakeImageResponse
import com.bakingbuddy.models.ingredients.IngredientHistory
import com.bakingbuddy.repositories.BakeStorageRepositoryImpl
import kotlin.uuid.Uuid

class BakeStorageService {
  private val bakeStorageRepository = BakeStorageRepositoryImpl()

  suspend fun uploadImageToBake(bakeId: Uuid, path: String, imageUrl: String): Unit =
    bakeStorageRepository.uploadImageToBake(bakeId, path, imageUrl)
}
