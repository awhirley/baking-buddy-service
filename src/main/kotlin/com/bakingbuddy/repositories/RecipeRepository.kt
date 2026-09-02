package com.bakingbuddy.repositories

import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.ingredients.UpdateIngredientPayload
import com.bakingbuddy.models.instructions.Instruction
import com.bakingbuddy.models.instructions.UpdateInstructionPayload
import com.bakingbuddy.models.recipes.CreateRecipePayload
import com.bakingbuddy.models.recipes.EditRecipePayload
import com.bakingbuddy.models.recipes.Recipe
import com.bakingbuddy.models.recipes.RecipeDetail
import kotlin.uuid.Uuid

interface RecipeRepository {
  suspend fun findById(id: Uuid): Recipe?

  suspend fun listAll(): List<RecipeDetail>

  suspend fun create(request: CreateRecipePayload): Recipe

  suspend fun editRecipe(
    id: Uuid,
    request: EditRecipePayload,
  ): Recipe

  suspend fun updateIngredient(
    ingredientId: Uuid,
    request: UpdateIngredientPayload,
  ): Ingredient

  suspend fun updateInstruction(
    instructionId: Uuid,
    request: UpdateInstructionPayload,
  ): Instruction

  suspend fun updateRecipeNotes(
    recipeId: Uuid,
    notes: String?,
  )

  suspend fun deleteRecipe(id: Uuid)
}
