package com.bakingbuddy.repositories

import com.bakingbuddy.models.CreateIngredientPayload
import com.bakingbuddy.models.CreateRecipePayload
import com.bakingbuddy.models.EditIngredientPayload
import com.bakingbuddy.models.EditInstructionPayload
import com.bakingbuddy.models.EditRecipePayload
import com.bakingbuddy.models.Ingredient
import com.bakingbuddy.models.Instruction
import com.bakingbuddy.models.Recipe
import com.bakingbuddy.models.RecipeDetail
import kotlin.uuid.Uuid

interface RecipeRepository {

    suspend fun findById(id: Uuid): RecipeDetail?
        
    suspend fun listAll(): List<Recipe>
    
    suspend fun create(request: CreateRecipePayload): RecipeDetail
    
    suspend fun editRecipe(id: Uuid, request: EditRecipePayload): RecipeDetail
        
    suspend fun editIngredient(ingredientId: Uuid, request: EditIngredientPayload): Ingredient
        
    suspend fun editInstruction(instructionId: Uuid, request: EditInstructionPayload): Instruction
    
    suspend fun updateRecipeNotes(recipeId: Uuid, notes: String?)
    
    suspend fun updateIngredientNotes(ingredientId: Uuid, notes: String?)
    
    suspend fun updateInstructionNotes(instructionId: Uuid, notes: String?)

    suspend fun deleteRecipe(id: Uuid): Boolean
}