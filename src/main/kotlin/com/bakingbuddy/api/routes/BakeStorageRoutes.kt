package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.FieldError
import com.bakingbuddy.api.errors.UnprocessableEntityException
import com.bakingbuddy.api.errors.ValidationException
import com.bakingbuddy.models.bakeStorage.BakeImageResponse
import com.bakingbuddy.plugins.SupabaseStorageClientKey
import com.bakingbuddy.services.BakeStorageService
import com.bakingbuddy.storage.SupabaseStorageClient
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlin.uuid.Uuid

fun Route.bakeStorageRoutes(storageClient: SupabaseStorageClient, bakeStorageService: BakeStorageService) {
  post("/api/bakes/{bakeId}/image") {
    val bakeId = call.parameters["bakeId"]?.let { Uuid.parse(it) }
      ?: throw BadRequestException("bakeId")

    val multipart = call.receiveMultipart()
    var imageBytes: ByteArray? = null
    var contentType: String? = null

    multipart.forEachPart { part ->
      if (part is PartData.FileItem) {
        contentType = part.contentType?.toString()
        imageBytes = part.provider().readRemaining().readByteArray()
      }
      part.release()
    }

    val bytes = imageBytes ?: throw ValidationException(listOf(FieldError(field = "imageBytes", message = "Bytes could not be pulled from call data")))
    val content = contentType ?: throw ValidationException(listOf(FieldError(field = "contentType", message = "contentType could not be pulled from call data")))

    val path = "$bakeId/${Uuid.random()}.${extensionForContentType(content)}"
    val imageUrl = storageClient.uploadImage(path, bytes, content)

    try {
      bakeStorageService.uploadImageToBake(bakeId, path, imageUrl)
    } catch (error: Exception) {
      throw UnprocessableEntityException(message = "Failure while uploading image to Bake $bakeId", mapOf("error" to error.localizedMessage) )
    }

    call.respond(HttpStatusCode.OK, BakeImageResponse(path, imageUrl))
  }

//  delete("/api/bakes/{bakeId}") {
    // when deleting a bake, look up its stored image path first,
    // then: storageClient.deleteImage(path)
    // ... then delete the bake row itself
//  }
}

fun extensionForContentType(contentType: String): String =
  when (contentType.lowercase()) {
    "image/jpeg", "image/jpg" -> "jpg"
    "image/png" -> "png"
    "image/webp" -> "webp"
    "image/heic" -> "heic"
    "image/heif" -> "heif"
    else -> throw BadRequestException("Unsupported image content type: $contentType")
  }