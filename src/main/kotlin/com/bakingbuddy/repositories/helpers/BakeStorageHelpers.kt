package com.bakingbuddy.repositories.helpers

import com.bakingbuddy.database.BakeImagesTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.uuid.Uuid

fun getImagePath(bakeImageId: Uuid?): String? {
  if (bakeImageId == null) return null
  val bakeImage =
    BakeImagesTable
      .selectAll()
      .where { BakeImagesTable.id eq bakeImageId }
      .singleOrNull()

  return bakeImage?.get(BakeImagesTable.path)
}
