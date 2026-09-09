package com.bakingbuddy.repositories

import com.bakingbuddy.models.bakeStorage.BakeImage
import kotlin.uuid.Uuid

interface BakeStorageRepository {
  suspend fun uploadImageToBake(
    bakeId: Uuid,
    path: String,
  ): Unit

  suspend fun getImagesForBake(bakeId: Uuid): List<BakeImage>

  suspend fun confirmPathBelongsToBake(
    bakeId: Uuid,
    path: String,
  ): Uuid

  suspend fun deleteImage(bakeImageId: Uuid): Unit
}
