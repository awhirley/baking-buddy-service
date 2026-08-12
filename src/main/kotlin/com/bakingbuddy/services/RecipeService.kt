package com.bakingbuddy.services

import com.bakingbuddy.models.CreateRecipePayload
import com.bakingbuddy.models.Recipe
import com.bakingbuddy.models.RecipeDetail
import com.bakingbuddy.repositories.RecipeRepositoryImpl
import kotlin.uuid.Uuid

class RecipeService {
    private val recipeRepository = RecipeRepositoryImpl()

    suspend fun getRecipe(id: Uuid): RecipeDetail? {
        return recipeRepository.findById(id)
    }
    
    suspend fun listRecipes(): List<Recipe> {
        return recipeRepository.listAll()
    }
    
    suspend fun createRecipe(request: CreateRecipePayload): RecipeDetail {
        return recipeRepository.create(request)
    }
}