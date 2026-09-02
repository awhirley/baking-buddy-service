package com.bakingbuddy.services

import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.ingredients.UpdateIngredientPayload
import com.bakingbuddy.models.instructions.Instruction
import com.bakingbuddy.models.instructions.UpdateInstructionPayload
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

  suspend fun updateIngredient(
    ingredientId: Uuid,
    request: UpdateIngredientPayload,
  ): Ingredient = recipeRepository.updateIngredient(ingredientId, request)

  suspend fun updateInstruction(
    instructionId: Uuid,
    request: UpdateInstructionPayload,
  ): Instruction = recipeRepository.updateInstruction(instructionId, request)

  suspend fun updateRecipeNotes(
    recipeId: Uuid,
    notes: String?,
  ) = recipeRepository.updateRecipeNotes(recipeId, notes)

  suspend fun deleteRecipe(id: Uuid) = recipeRepository.deleteRecipe(id)
}
