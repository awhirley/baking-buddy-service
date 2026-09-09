package com.bakingbuddy.repositories

import com.bakingbuddy.models.recipes.Recipe
import com.bakingbuddy.models.recipes.RecipeDetail

interface FilterRepository {
  suspend fun listTools() : Set<String>

  suspend fun listTags() : Set<String>

  suspend fun listSources() : Set<String>

  suspend fun listSourceTypes() : Set<String>
}