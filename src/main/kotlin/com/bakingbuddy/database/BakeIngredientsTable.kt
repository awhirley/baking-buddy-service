package com.bakingbuddy.database

import org.jetbrains.exposed.v1.core.Table

object BakeIngredientsTable : Table("bake_ingredients") {
  val id = uuid("id")
  val bake_id = uuid("bake_id").references(BakesTable.id)
  val ingredient_delta_id = uuid("ingredient_delta_id").references(IngredientDeltaTable.id)
  val amount = text("amount")
  val name = text("name")
  val notes = text("notes").nullable()
  val completed_bake_delta_id = uuid("completed_bake_delta_id").nullable()
  val order = integer("order")

  override val primaryKey = PrimaryKey(id)
}
