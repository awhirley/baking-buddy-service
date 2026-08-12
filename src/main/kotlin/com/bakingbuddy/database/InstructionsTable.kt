package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.javatime.timestamp

object Instructions : Table("instructions") {
    val id = uuid("id")
    val recipe_id = uuid("recipe_id")
    val best_version = integer("best_version")
    val notes = text("notes")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}