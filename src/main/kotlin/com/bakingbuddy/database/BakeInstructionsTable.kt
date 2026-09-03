package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table

object BakeInstructionsTable : Table("bake_instructions") {
  val id = uuid("id")
  val bake_id = uuid("bake_id").references(BakesTable.id)
  val instruction_delta_id = uuid("instruction_delta_id").references(InstructionDeltaTable.id)
  val description = text("description").nullable()
  val notes = text("notes").nullable()
  val completed_bake_delta_id = uuid("completed_bake_delta_id").nullable()
  val order = integer("order").nullable()

  override val primaryKey = PrimaryKey(id)
}
