package com.bakingbuddy.plugins

import com.bakingbuddy.storage.SupabaseStorageClient
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.server.application.Application
import io.ktor.util.AttributeKey

val SupabaseStorageClientKey = AttributeKey<SupabaseStorageClient>("SupabaseStorageClient")

data class SupabaseConfig(
  val url: String,
  val secretKey: String,
  val storageBucket: String,
)

fun Application.configureSupabase() {
  val dotenv = Dotenv.configure().ignoreIfMissing().load()
  val environment: String =
    System.getenv("APP_ENV") ?: dotenv.get("APP_ENV") ?: error("APP_ENV is not set.")

  val config =
    SupabaseConfig(
      url = readEnv("SUPABASE_URL", environment, dotenv),
      secretKey = readEnv("SUPABASE_SECRET_KEY", environment, dotenv),
      storageBucket = readEnv("SUPABASE_STORAGE_BUCKET", environment, dotenv),
    )

  val httpClient = HttpClient(CIO)
  val storageClient = SupabaseStorageClient(httpClient, config)

  attributes.put(SupabaseStorageClientKey, storageClient)
}

fun readEnv(
  key: String,
  environment: String,
  dotenv: Dotenv,
): String {
  val value =
    when (environment) {
      "production" -> System.getenv(key)
      "development" -> dotenv.get(key)
      else -> error("Unknown APP_ENV: $environment")
    }
  return value ?: error("$key is not set")
}
