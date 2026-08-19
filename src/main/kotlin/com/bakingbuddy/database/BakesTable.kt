package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp

object BakesTable : Table("bakes") {
    val id = uuid("id")
    val recipe_id = uuid("recipe_id").references(RecipesTable.id)
    val date = date("date").nullable()
    val results = text("results").nullable()
    val elevation = integer("elevation").nullable()
    val notes = text("notes").nullable()
    val created_at = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
