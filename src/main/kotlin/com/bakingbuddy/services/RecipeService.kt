package com.bakingbuddy.services

import com.bakingbuddy.models.ingredients.CreateIngredientPayload
import com.bakingbuddy.models.recipes.CreateRecipePayload
import com.bakingbuddy.models.ingredients.EditIngredientPayload
import com.bakingbuddy.models.instructions.EditInstructionPayload
import com.bakingbuddy.models.recipes.EditRecipePayload
import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.instructions.Instruction
import com.bakingbuddy.models.recipes.Recipe
import com.bakingbuddy.models.recipes.RecipeDetail
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
    
    suspend fun updateRecipeNotes(recipeId: Uuid, notes: String?) {
        return recipeRepository.updateRecipeNotes(recipeId, notes)
    }
    
    suspend fun updateIngredientNotes(ingredientId: Uuid, notes: String?) {
        return recipeRepository.updateIngredientNotes(ingredientId, notes)
    }
    
    suspend fun updateInstructionNotes(instructionId: Uuid, notes: String?) {
        return recipeRepository.updateInstructionNotes(instructionId, notes)
    }
    
    suspend fun deleteRecipe(id: Uuid): Boolean {
        return recipeRepository.deleteRecipe(id)
    }
}