package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.javatime.timestamp

object BakeInstructions : Table("bake_instruction") {
    val id = uuid("id")
    val bake_id = uuid("bake_id").references(Bakes.id)
    val instruction_delta_id = uuid("instruction_delta_id").references(InstructionDelta.id)

    override val primaryKey = PrimaryKey(id)
}