package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.FieldError
import com.bakingbuddy.api.errors.ValidationException
import com.bakingbuddy.plugins.SupabaseStorageClientKey
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

fun Route.bakeStorageRoutes(storageClient: SupabaseStorageClient) {
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

    // persist imageUrl/path on the bake row via your repository
    call.respond(HttpStatusCode.OK, mapOf("imageUrl" to imageUrl))
  }

  delete("/api/bakes/{bakeId}") {
    // when deleting a bake, look up its stored image path first,
    // then: storageClient.deleteImage(path)
    // ... then delete the bake row itself
  }
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