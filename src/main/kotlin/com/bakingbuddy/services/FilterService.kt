package com.bakingbuddy.services

import com.bakingbuddy.repositories.FilterRepositoryImpl

class FilterService {
  private val filterRepository = FilterRepositoryImpl()

  suspend fun listTools() = filterRepository.listTools()

  suspend fun listTags() = filterRepository.listTags()

  suspend fun listSources() = filterRepository.listSources()

  suspend fun listSourceTypes() = filterRepository.listSourceTypes()
}
