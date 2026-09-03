package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object InstructionDeltaTable : Table("instruction_delta") {
  val id = uuid("id")
  val instruction_id = uuid("instruction_id")
  val version = integer("version")
  val description = text("description")
  val notes = text("notes").nullable()
  val source_bake_id = uuid("source_bake_id").nullable()
  val created_at = timestamp("created_at")
  val order = integer("order").nullable()

  override val primaryKey = PrimaryKey(id)
}
