package com.bakingbuddy.api.routes

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.models.bakeStorage.DeleteImagePayload
import com.bakingbuddy.services.BakeStorageService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlin.uuid.Uuid

fun Route.bakeStorageRoutes(bakeStorageService: BakeStorageService) {
  post("/api/bakes/{bakeId}/image") {
    val bakeId =
      call.parameters["bakeId"]?.let { Uuid.parse(it) }
        ?: throw BadRequestException("bakeId")
    val multipart = call.receiveMultipart()

    val response = bakeStorageService.uploadImageToBake(bakeId, multipart)
    call.respond(HttpStatusCode.OK, response)
  }

  get("/api/bakes/{bakeId}/image") {
    val bakeId =
      call.parameters["bakeId"]?.let { Uuid.parse(it) }
        ?: throw BadRequestException("bakeId")

    val bakeImages = bakeStorageService.getImagesForBake(bakeId)
    call.respond(HttpStatusCode.OK, bakeImages)
  }

  delete("/api/bakes/{bakeId}/image") {
    val bakeId =
      call.parameters["bakeId"]?.let { Uuid.parse(it) }
        ?: throw BadRequestException("bakeId")

    val payload = call.receive<DeleteImagePayload>()

    bakeStorageService.deleteImage(bakeId, payload.path)
    call.respond(HttpStatusCode.OK)
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

@Suppress("MagicNumber")
fun detectImageType(
  bytes: ByteArray,
  declaredContentType: String?,
): String {
  if (declaredContentType != null && declaredContentType != "application/octet-stream") {
    return declaredContentType
  }
  return when {
    bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "image/png"
    bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
    bytes.size >= 12 && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
    else -> throw BadRequestException("Could not determine image type")
  }
}
