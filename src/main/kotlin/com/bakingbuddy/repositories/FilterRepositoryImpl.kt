package com.bakingbuddy.repositories

import com.bakingbuddy.database.RecipesTable
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class FilterRepositoryImpl : FilterRepository {
  override suspend fun listTools(): Set<String> =
    transaction {
      RecipesTable
        .select(RecipesTable.tools)
        .flatMap { it[RecipesTable.tools] }
        .toSortedSet()
    }

  override suspend fun listTags(): Set<String> =
    transaction {
      RecipesTable
        .select(RecipesTable.tags)
        .flatMap { it[RecipesTable.tags] }
        .toSortedSet()
    }

  override suspend fun listSources(): Set<String> =
    transaction {
      RecipesTable
        .select(RecipesTable.recipe_source)
        .mapNotNull { it[RecipesTable.recipe_source] }
        .toSortedSet()
    }

  override suspend fun listSourceTypes(): Set<String> =
    transaction {
      RecipesTable
        .select(RecipesTable.recipe_source_type)
        .mapNotNull { it[RecipesTable.recipe_source_type] }
        .toSortedSet()
    }
}
