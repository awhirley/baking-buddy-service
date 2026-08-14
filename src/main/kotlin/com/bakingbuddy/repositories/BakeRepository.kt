package com.bakingbuddy.repositories

import com.bakingbuddy.models.bakes.Bake
import com.bakingbuddy.models.bakes.BakeDetail
import com.bakingbuddy.models.bakes.CreateBakePayload
import kotlin.uuid.Uuid

interface BakeRepository {
    suspend fun createBake(payload: CreateBakePayload): Bake
    
    suspend fun listBakesWithProcedure(recipeId: Uuid): List<Bake>
    
    suspend fun listBakes(recipeId: Uuid): List<BakeDetail>
}