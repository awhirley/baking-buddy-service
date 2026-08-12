package com.bakingbuddy.repositories

import com.bakingbuddy.models.CreateRecipePayload
import com.bakingbuddy.models.Recipe
import java.util.UUID

interface RecipeRepository {

    suspend fun findById(id: UUID): Recipe?
        
    suspend fun listAll(): List<Recipe>
    
    suspend fun create(request: CreateRecipePayload): Recipe
}