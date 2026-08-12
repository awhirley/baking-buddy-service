package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.javatime.timestamp

object Recipes : Table("recipes") {
    val id = uuid("id")
    val name = text("name")
    val description = text("description")
    val created_at = timestamp("created_at")
    val recipe_source = text("recipe_source")
    val tags = array<String>("tags", TextColumnType())
    val tools = array<String>("tools", TextColumnType())

    override val primaryKey = PrimaryKey(id)
}