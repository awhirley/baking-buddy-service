package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object InstructionDelta : Table("instruction_delta") {
    val id = uuid("id")
    val instruction_id = uuid("instruction_id")
    val version = integer("version")
    val description = text("description")
    val notes = text("notes")
    val created_at = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
