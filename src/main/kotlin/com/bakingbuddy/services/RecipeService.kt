package com.bakingbuddy.services

import com.bakingbuddy.models.CreateRecipeRequest
import com.bakingbuddy.models.Recipe
import com.bakingbuddy.repositories.RecipeRepositoryImpl
import java.util.UUID

class RecipeService {
    private val recipeRepository = RecipeRepositoryImpl()

    suspend fun getRecipe(id: UUID): Recipe? {
        return recipeRepository.findById(id)
    }
    
    suspend fun listRecipes(): List<Recipe> {
        return recipeRepository.listAll()
    }
    
    suspend fun createRecipe(request: CreateRecipeRequest): Recipe {
        return recipeRepository.create(request)
    }
}