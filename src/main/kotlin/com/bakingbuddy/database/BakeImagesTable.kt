package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object BakeImagesTable : Table("bake_images") {
  val id = uuid("id")
  val bake_id = uuid("bake_id").references(RecipesTable.id)
  val path = text("path")
  val created_at = timestamp("created_at")

  override val primaryKey = PrimaryKey(id)
}
