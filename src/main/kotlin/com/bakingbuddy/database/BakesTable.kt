package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.javatime.timestamp

object Bakes : Table("bakes") {
    val id = uuid("id")
    val recipe_id = uuid("recipe_id").references(Recipes.id)
    val date = timestamp("date")
    val results = text("results")
    val elevation = integer("elevation")
    val notes = text("notes").nullable()
    val created_at = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}