package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.javatime.timestamp

object RecipesTable : Table("recipes") {
  val id = uuid("id")
  val name = text("name")
  val description = text("description").nullable()
  val created_at = timestamp("created_at")
  val recipe_source = text("recipe_source").nullable()
  val recipe_source_type = text("recipe_source_type").nullable()
  val tags = array<String>("tags", TextColumnType())
  val tools = array<String>("tools", TextColumnType())
  val notes = text("notes").nullable()
  val favorite = bool("favorite")
  val difficulty_rating = integer("difficulty_rating").nullable()

  override val primaryKey = PrimaryKey(id)
}
