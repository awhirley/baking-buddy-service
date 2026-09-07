package com.bakingbuddy.repositories

import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.database.BakeImagesTable
import com.bakingbuddy.database.BakeIngredientsTable
import com.bakingbuddy.models.bakeStorage.BakeImage
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import kotlin.uuid.Uuid

class BakeStorageRepositoryImpl : BakeStorageRepository {
  override suspend fun uploadImageToBake(
    bakeId: Uuid,
    path: String,
  ) = transaction {
    val bakeImageId = Uuid.random()
    val createdAt = Instant.now()

    BakeImagesTable.insert {
      it[BakeImagesTable.id] = bakeImageId
      it[BakeImagesTable.bake_id] = bakeId
      it[BakeImagesTable.created_at] = createdAt
      it[BakeImagesTable.path] = path
    }

    return@transaction
  }

  override suspend fun getImagesForBake(bakeId: Uuid): List<BakeImage> =
    transaction {
      val bakeImageRows =
        BakeImagesTable
          .selectAll()
          .where { BakeImagesTable.bake_id eq bakeId }
          .orderBy(BakeImagesTable.created_at to SortOrder.DESC)

      bakeImageRows.map {
        BakeImage(
          bakeId = it[BakeImagesTable.bake_id],
          path = it[BakeImagesTable.path],
          createdAt = it[BakeImagesTable.created_at],
          imageUrl = null,
        )
      }
    }

  override suspend fun confirmPathBelongsToBake(bakeId: Uuid, path: String): Uuid {
    val id = transaction {
      val bakeImage = BakeImagesTable
        .selectAll()
        .where { (BakeImagesTable.bake_id eq bakeId) and (BakeImagesTable.path eq path) }
        .singleOrNull() ?: throw NotFoundException("Bake image", "$bakeId/$path")

      bakeImage[BakeImagesTable.id]
    }
    return id
  }

  override suspend fun deleteImage(bakeImageId: Uuid) {
    transaction {
      BakeImagesTable.deleteWhere { BakeImagesTable.id eq bakeImageId }
    }
  }
}
