package com.bakingbuddy.repositories

import com.bakingbuddy.database.Recipes
import com.bakingbuddy.models.Recipe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.uuid.toKotlinUuid

class RecipeRepositoryImpl : RecipeRepository {

    override suspend fun findById(id: UUID): Recipe? {
        return transaction {
            Recipes
                .selectAll()
                .where { Recipes.id eq id.toKotlinUuid() }
                .map { row ->
                    Recipe(
                        id = row[Recipes.id].toString(),
                        name = row[Recipes.name],
                        description = row[Recipes.description],
                        createdAt = null,
                    )
                }
                .singleOrNull()
        }
    }
    
    override suspend fun listAll(): List<Recipe> {
        return transaction {
            Recipes
                .selectAll()
                .map { row ->
                    Recipe(
                        id = row[Recipes.id].toString(),
                        name = row[Recipes.name],
                        description = row[Recipes.description],
                        createdAt = null,
                    )
                }
        }
    }
}