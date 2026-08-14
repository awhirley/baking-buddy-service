package com.bakingbuddy.repositories

import com.bakingbuddy.models.bakes.Bake
import com.bakingbuddy.models.bakes.CreateBakePayload
import kotlin.uuid.Uuid

interface BakeRepository {
    suspend fun createBake(payload: CreateBakePayload): Bake
}