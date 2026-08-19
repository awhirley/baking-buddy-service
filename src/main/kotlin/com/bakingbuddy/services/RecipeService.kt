package com.bakingbuddy.services

import com.bakingbuddy.models.ingredients.EditIngredientPayload
import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.instructions.EditInstructionPayload
import com.bakingbuddy.models.instructions.Instruction
import com.bakingbuddy.models.recipes.CreateRecipePayload
import com.bakingbuddy.models.recipes.EditRecipePayload
import com.bakingbuddy.models.recipes.Recipe
import com.bakingbuddy.models.recipes.RecipeDetail
import com.bakingbuddy.repositories.RecipeRepositoryImpl
import kotlin.uuid.Uuid

class RecipeService {
    private val recipeRepository = RecipeRepositoryImpl()

    suspend fun getRecipe(id: Uuid): Recipe? = recipeRepository.findById(id)

    suspend fun listRecipes(): List<RecipeDetail> = recipeRepository.listAll()

    suspend fun createRecipe(request: CreateRecipePayload): Recipe = recipeRepository.create(request)

    suspend fun editRecipe(
        recipeId: Uuid,
        request: EditRecipePayload,
    ): Recipe = recipeRepository.editRecipe(recipeId, request)

    suspend fun editIngredient(
        ingredientId: Uuid,
        request: EditIngredientPayload,
    ): Ingredient = recipeRepository.editIngredient(ingredientId, request)

    suspend fun editInstruction(
        instructionId: Uuid,
        request: EditInstructionPayload,
    ): Instruction = recipeRepository.editInstruction(instructionId, request)

    suspend fun updateRecipeNotes(
        recipeId: Uuid,
        notes: String?,
    ) = recipeRepository.updateRecipeNotes(recipeId, notes)

    suspend fun updateIngredientNotes(
        ingredientId: Uuid,
        notes: String?,
    ) = recipeRepository.updateIngredientNotes(ingredientId, notes)

    suspend fun updateInstructionNotes(
        instructionId: Uuid,
        notes: String?,
    ) = recipeRepository.updateInstructionNotes(instructionId, notes)

    suspend fun deleteRecipe(id: Uuid) = recipeRepository.deleteRecipe(id)
}
