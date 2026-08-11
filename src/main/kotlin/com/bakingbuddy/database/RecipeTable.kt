package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table

object Recipes : Table("recipes") {
    val id = uuid("id")
    val name = text("name")
    val description = text("description")
    // val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}