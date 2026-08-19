package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object Ingredients : Table("ingredients") {
    val id = uuid("id")
    val recipe_id = uuid("recipe_id")
    val best_version = integer("best_version")
    val created_at = timestamp("created_at")
    val notes = text("notes").nullable()

    override val primaryKey = PrimaryKey(id)
}
