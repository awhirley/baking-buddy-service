package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object BakeRatingsTable : Table("bake_ratings") {
  val id = uuid("id")
  val bake_id = uuid("bake_id").references(BakesTable.id)
  val created_at = timestamp("created_at")

  val overall = integer("overall").nullable()
  val taste = integer("taste").nullable()
  val texture = integer("texture").nullable()
  val appearance = integer("appearance").nullable()
  val rise_structure = integer("rise_structure").nullable()
  val difficulty = integer("difficulty").nullable()

  override val primaryKey = PrimaryKey(id)
}
