package com.bakingbuddy.services

import com.bakingbuddy.api.errors.FieldError
import com.bakingbuddy.api.errors.UnprocessableEntityException
import com.bakingbuddy.api.errors.ValidationException
import com.bakingbuddy.api.routes.detectImageType
import com.bakingbuddy.api.routes.extensionForContentType
import com.bakingbuddy.models.bakeStorage.BakeImage
import com.bakingbuddy.models.bakeStorage.CreateBakeImageResponse
import com.bakingbuddy.repositories.BakeStorageRepositoryImpl
import com.bakingbuddy.storage.SupabaseStorageClient
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.routing.path
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import java.io.IOException
import kotlin.uuid.Uuid

class BakeStorageService(
  val storageClient: SupabaseStorageClient,
) {
  private val bakeStorageRepository = BakeStorageRepositoryImpl()

  suspend fun uploadImageToBake(
    bakeId: Uuid,
    multiPartData: MultiPartData,
  ): CreateBakeImageResponse {
    var imageBytes: ByteArray? = null
    var contentType: String? = null

    multiPartData.forEachPart { part ->
      if (part is PartData.FileItem) {
        contentType = part.contentType?.toString()
        imageBytes = part.provider().readRemaining().readByteArray()
      }
      part.release()
    }

    val bytes =
      imageBytes
        ?: throw ValidationException(
          listOf(FieldError(field = "imageBytes", message = "Bytes could not be pulled from call data")),
        )
    val detectedContentType = detectImageType(bytes, contentType)

    val path = "$bakeId/${Uuid.random()}.${extensionForContentType(detectedContentType)}"
    val imageUrl = storageClient.uploadImage(path, bytes, detectedContentType)

    try {
      bakeStorageRepository.uploadImageToBake(bakeId, path)
    } catch (error: IOException) {
      throw UnprocessableEntityException(
        message = "Failure while uploading image to Bake $bakeId",
        details = mapOf("error" to (error.message ?: "Unknown IO error")),
        cause = error,
      )
    }

    return CreateBakeImageResponse(path, imageUrl)
  }

  suspend fun getImagesForBake(bakeId: Uuid): List<BakeImage> {
    val images = bakeStorageRepository.getImagesForBake(bakeId)
    return images.map { image ->
      BakeImage(
        bakeId = image.bakeId,
        path = image.path,
        imageUrl = storageClient.getUrlForPath(image.path),
        createdAt = image.createdAt,
      )
    }
  }
}
