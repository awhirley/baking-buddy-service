package com.bakingbuddy.services

import com.bakingbuddy.models.bakes.Bake
import com.bakingbuddy.models.bakes.BakeDetail
import com.bakingbuddy.models.bakes.UpdateBakePayload
import com.bakingbuddy.repositories.BakeRepositoryImpl
import kotlin.uuid.Uuid

class BakeService {
  private val bakeRepository = BakeRepositoryImpl()

  suspend fun createBake(recipeId: Uuid): Bake = bakeRepository.createBake(recipeId)

  suspend fun listBakes(recipeId: Uuid): List<BakeDetail> = bakeRepository.listBakes(recipeId)

  suspend fun listBakesWithProcedure(recipeId: Uuid): List<Bake> = bakeRepository.listBakesWithProcedure(recipeId)

  suspend fun updateBake(payload: UpdateBakePayload) = bakeRepository.updateBake(payload)

  suspend fun deleteBake(id: Uuid) = bakeRepository.deleteBake(id)

  suspend fun updateBakeInstruction(
    bakeId: Uuid,
    instructionDeltaId: Uuid,
    description: String,
  ) = bakeRepository.updateBakeInstruction(bakeId, instructionDeltaId, description)
}
