package com.bakingbuddy.repositories

import com.bakingbuddy.models.bakeStorage.BakeImageResponse
import kotlin.uuid.Uuid

class BakeStorageRepositoryImpl : BakeStorageRepository {
  override suspend fun uploadImageToBake(id: Uuid, path: String, imageUrl: String): Unit {
    // upload to DB
  }
}