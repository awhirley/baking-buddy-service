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
import com.bakingbuddy.storage.SupabaseStorageClient
import kotlin.uuid.Uuid

class RecipeService(
  val storageClient: SupabaseStorageClient,
) {
  private val recipeRepository = RecipeRepositoryImpl()

  suspend fun getRecipe(id: Uuid): Recipe? {
    val recipe = recipeRepository.findById(id) ?: return null
    return recipe.copy(
      details =
        recipe.details.copy(
          displayImage = recipe.details.displayImage?.let { storageClient.getUrlForPath(it) },
        ),
    )
  }

  suspend fun listRecipes(): List<RecipeDetail> =
    recipeRepository.listAll().map { detail ->
      detail.copy(
        displayImage = detail.displayImage?.let { storageClient.getUrlForPath(it) },
      )
    }

  suspend fun createRecipe(request: CreateRecipePayload): Recipe = recipeRepository.create(request)

  suspend fun editRecipe(
    recipeId: Uuid,
    request: EditRecipePayload,
  ): Recipe {
    val recipe = recipeRepository.editRecipe(recipeId, request)
    return recipe.copy(
      details =
        recipe.details.copy(
          displayImage = recipe.details.displayImage?.let { storageClient.getUrlForPath(it) },
        ),
    )
  }

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
