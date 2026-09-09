package com.bakingbuddy.repositories

interface FilterRepository {
  suspend fun listTools(): Set<String>

  suspend fun listTags(): Set<String>

  suspend fun listSources(): Set<String>

  suspend fun listSourceTypes(): Set<String>
}
