package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.javatime.timestamp

object BakeIngredients : Table("bake_ingredients") {
    val id = uuid("id")
    val bake_id = uuid("bake_id").references(Bakes.id)
    val ingredient_delta_id = uuid("ingredient_delta_id").references(IngredientDelta.id)

    override val primaryKey = PrimaryKey(id)
}