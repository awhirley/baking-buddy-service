package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table

object BakeInstructionsTable : Table("bake_instructions") {
  val id = uuid("id")
  val bake_id = uuid("bake_id").references(BakesTable.id)
  val instruction_id = uuid("instruction_id").references(InstructionsTable.id)
  val description = text("description").nullable()
  val notes = text("notes").nullable()

  override val primaryKey = PrimaryKey(id)
}
