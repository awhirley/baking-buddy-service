package com.bakingbuddy.services

import com.bakingbuddy.models.bakes.Bake
import com.bakingbuddy.models.bakes.BakeDetail
import com.bakingbuddy.models.bakes.CreateBakePayload
import com.bakingbuddy.repositories.BakeRepositoryImpl
import kotlin.uuid.Uuid

class BakeService {
    private val bakeRepository = BakeRepositoryImpl()
    
    suspend fun createBake(payload: CreateBakePayload): Bake {
        return bakeRepository.createBake(payload)
    }
    
    suspend fun listBakes(recipeId: Uuid): List<BakeDetail> {
        return bakeRepository.listBakes(recipeId)
    }
    
    suspend fun listBakesWithProcedure(recipeId: Uuid): List<Bake> {
        return bakeRepository.listBakesWithProcedure(recipeId)
    }
}