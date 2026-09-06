package com.bakingbuddy.storage

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.plugins.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class SupabaseStorageClient(
  private val httpClient: HttpClient,
  private val config: SupabaseConfig,
) {
  suspend fun uploadImage(
    path: String,
    bytes: ByteArray,
    contentType: String,
  ): String {
    val response: HttpResponse =
      httpClient.post("${config.url}/storage/v1/object/${config.storageBucket}/$path") {
        header("apikey", config.secretKey)
        header("Authorization", "Bearer ${config.secretKey}")
        contentType(ContentType.parse(contentType))
        setBody(bytes)
      }

    if (!response.status.isSuccess()) {
      throw BadRequestException("Failed to upload image: ${response.status}")
    }

    return getUrlForPath(path)
  }

  suspend fun deleteImage(path: String) {
    val response: HttpResponse =
      httpClient.delete("${config.url}/storage/v1/object/${config.storageBucket}/$path") {
        header("Authorization", "Bearer ${config.secretKey}")
      }

    if (!response.status.isSuccess()) {
      throw BadRequestException("Failed to delete image: ${response.status}")
    }
  }

  suspend fun getUrlForPath(path: String): String =
    "${config.url}/storage/v1/object/public/${config.storageBucket}/$path"
}
