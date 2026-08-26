package com.bakingbuddy.repositories

import com.bakingbuddy.models.bakes.Bake
import com.bakingbuddy.models.bakes.BakeDetail
import com.bakingbuddy.models.bakes.UpdateBakeIngredientPayload
import com.bakingbuddy.models.bakes.UpdateBakeInstructionPayload
import com.bakingbuddy.models.bakes.UpdateBakePayload
import kotlin.uuid.Uuid

interface BakeRepository {
  suspend fun createBake(recipeId: Uuid): Bake

  suspend fun listBakesWithProcedure(recipeId: Uuid): List<Bake>

  suspend fun listBakes(recipeId: Uuid): List<BakeDetail>

  suspend fun updateBake(payload: UpdateBakePayload)

  suspend fun deleteBake(id: Uuid)

  suspend fun updateBakeInstruction(
    bakeId: Uuid,
    instructionDeltaId: Uuid,
    payload: UpdateBakeInstructionPayload,
  )
  
  suspend fun updateBakeIngredient(
    bakeId: Uuid,
    ingredientDeltaId: Uuid,
    payload: UpdateBakeIngredientPayload,
  )
}
