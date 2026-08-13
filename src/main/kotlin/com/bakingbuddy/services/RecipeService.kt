package com.bakingbuddy.services

import com.bakingbuddy.models.CreateIngredientPayload
import com.bakingbuddy.models.CreateRecipePayload
import com.bakingbuddy.models.EditIngredientPayload
import com.bakingbuddy.models.EditInstructionPayload
import com.bakingbuddy.models.EditRecipePayload
import com.bakingbuddy.models.Ingredient
import com.bakingbuddy.models.Instruction
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
    
    suspend fun editRecipe(recipeId: Uuid, request: EditRecipePayload): RecipeDetail {
        return recipeRepository.editRecipe(recipeId, request)
    }
    
    suspend fun editIngredient(ingredientId: Uuid, request: EditIngredientPayload): Ingredient {
        return recipeRepository.editIngredient(ingredientId, request)
    }
    
    suspend fun editInstruction(instructionId: Uuid, request: EditInstructionPayload): Instruction {
        return recipeRepository.editInstruction(instructionId, request)
    }
}