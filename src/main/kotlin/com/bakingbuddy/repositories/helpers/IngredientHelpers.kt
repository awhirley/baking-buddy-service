package com.bakingbuddy.repositories.helpers

import com.bakingbuddy.api.errors.DataIntegrityException
import com.bakingbuddy.database.IngredientDeltaTable
import com.bakingbuddy.database.IngredientsTable
import com.bakingbuddy.models.ingredients.CreateIngredientPayload
import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.ingredients.IngredientDeltaEntry
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import kotlin.uuid.Uuid

fun createIngredients(
  recipeId: Uuid,
  request: List<CreateIngredientPayload>,
): List<Ingredient> =
  request.mapIndexed { index, ingredient ->
    val ingredientId = Uuid.random()
    val createdAt = Instant.now()
    val order = (index + 1) * 10

    val ingredientStatement =
      IngredientsTable.insert {
        it[IngredientsTable.id] = ingredientId
        it[IngredientsTable.recipe_id] = recipeId
        it[IngredientsTable.best_version] = 1
        it[IngredientsTable.created_at] = createdAt
      }

    IngredientDeltaTable.insert {
      it[IngredientDeltaTable.ingredient_id] = ingredientId
      it[IngredientDeltaTable.version] = 1
      it[IngredientDeltaTable.amount] = ingredient.amount
      it[IngredientDeltaTable.name] = ingredient.name
      it[IngredientDeltaTable.created_at] = createdAt
      it[IngredientDeltaTable.order] = order
    }

    Ingredient(
      id = ingredientId,
      recipeId = recipeId,
      bestVersion = 1,
      amount = ingredient.amount,
      name = ingredient.name,
      notes = null,
      createdAt = ingredientStatement[IngredientsTable.created_at],
      order = order,
    )
  }

fun getIngredientsForRecipe(recipeId: Uuid): List<Ingredient> {
  val ingredients =
    transaction {
      val ingredientJoin =
        IngredientsTable.join(
          IngredientDeltaTable,
          JoinType.INNER,
          onColumn = IngredientsTable.id,
          otherColumn = IngredientDeltaTable.ingredient_id,
          additionalConstraint = { IngredientDeltaTable.version eq IngredientsTable.best_version },
        )

      ingredientJoin
        .selectAll()
        .where { IngredientsTable.recipe_id eq recipeId }
        .orderBy(IngredientDeltaTable.order to SortOrder.ASC)
        .map { row ->
          Ingredient(
            id = row[IngredientsTable.id],
            recipeId = row[IngredientsTable.recipe_id],
            bestVersion = row[IngredientsTable.best_version],
            notes = row[IngredientDeltaTable.notes],
            createdAt = row[IngredientsTable.created_at],
            amount = row[IngredientDeltaTable.amount],
            name = row[IngredientDeltaTable.name],
            order = row[IngredientDeltaTable.order],
          )
        }
    }
  val ingredientConceptCount =
    IngredientsTable
      .selectAll()
      .where { IngredientsTable.recipe_id eq recipeId }
      .count()

  if (ingredients.size.toLong() != ingredientConceptCount) {
    throw DataIntegrityException(
      "Missing ingredient_delta row for best_version on one or more ingredients of recipe $recipeId",
    )
  }

  return ingredients
}

data class BestIngredientDelta(
  val bakeIngredientId: Uuid,
  val bestDelta: IngredientDeltaEntry,
)
