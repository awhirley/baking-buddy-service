package com.bakingbuddy.repositories

import com.bakingbuddy.models.CreateRecipePayload
import com.bakingbuddy.models.Recipe
import com.bakingbuddy.models.RecipeDetail
import kotlin.uuid.Uuid

interface RecipeRepository {

    suspend fun findById(id: Uuid): RecipeDetail?
        
    suspend fun listAll(): List<Recipe>
    
    suspend fun create(request: CreateRecipePayload): RecipeDetail
}