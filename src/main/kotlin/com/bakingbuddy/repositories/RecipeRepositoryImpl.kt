package com.bakingbuddy.repositories

import com.bakingbuddy.database.Recipes
import com.bakingbuddy.models.CreateRecipePayload
import com.bakingbuddy.models.Recipe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
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
                        id = row[Recipes.id],
                        name = row[Recipes.name],
                        description = row[Recipes.description],
                        recipeSource = row[Recipes.recipe_source],
                        tags = row[Recipes.tags],
                        tools = row[Recipes.tools],
                        createdAt = row[Recipes.created_at],
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
                        id = row[Recipes.id],
                        name = row[Recipes.name],
                        description = row[Recipes.description],
                        recipeSource = row[Recipes.recipe_source],
                        tags = row[Recipes.tags],
                        tools = row[Recipes.tools],
                        createdAt = row[Recipes.created_at],
                    )
                }
        }
    }
    
    override suspend fun create(request: CreateRecipePayload): Recipe {
        return transaction {
            val statement = Recipes.insert {
                it[Recipes.name] = request.name
                it[Recipes.description] = request.description
                it[Recipes.recipe_source] = request.recipeSource.orEmpty()
                it[Recipes.tags] = request.tags.orEmpty()
                it[Recipes.tools] = request.tools.orEmpty()
            }
            
            // TODO, we need a lot more here, creating rows in other tables
            // and we need to update both Recipe model and CreateRecipePayload to include those other tables

            Recipe(
                id = statement[Recipes.id],
                name = request.name,
                description = request.description,
                recipeSource = request.recipeSource,
                tags = request.tags,
                tools = request.tools,
                createdAt = statement[Recipes.created_at],
            )
        }
    }
}