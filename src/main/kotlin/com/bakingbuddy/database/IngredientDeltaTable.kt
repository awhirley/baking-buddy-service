package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.javatime.timestamp

object IngredientDelta : Table("ingredient_delta") {
    val id = uuid("id")
    val ingredient_id = uuid("ingredient_id")
    val version = integer("version")
    val amount = text("amount")
    val name = text("name")
    val notes = text("notes")
    val created_at = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}