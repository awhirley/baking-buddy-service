package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table

object BakeInstructionsTable : Table("bake_instructions") {
  val id = uuid("id")
  val bake_id = uuid("bake_id").references(BakesTable.id)
  val instruction_delta_id = uuid("instruction_delta_id").references(InstructionDeltaTable.id)

  override val primaryKey = PrimaryKey(id)
}
