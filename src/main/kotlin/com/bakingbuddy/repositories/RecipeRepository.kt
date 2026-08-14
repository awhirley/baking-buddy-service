package com.bakingbuddy.repositories

import com.bakingbuddy.models.ingredients.CreateIngredientPayload
import com.bakingbuddy.models.recipes.CreateRecipePayload
import com.bakingbuddy.models.ingredients.EditIngredientPayload
import com.bakingbuddy.models.instructions.EditInstructionPayload
import com.bakingbuddy.models.recipes.EditRecipePayload
import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.instructions.Instruction
import com.bakingbuddy.models.recipes.Recipe
import com.bakingbuddy.models.recipes.RecipeDetail
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