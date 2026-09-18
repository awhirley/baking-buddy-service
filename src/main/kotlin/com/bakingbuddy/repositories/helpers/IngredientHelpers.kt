package com.bakingbuddy.repositories.helpers

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.DataIntegrityException
import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.database.IngredientDeltaTable
import com.bakingbuddy.database.IngredientsTable
import com.bakingbuddy.models.ingredients.AddIngredientPayload
import com.bakingbuddy.models.ingredients.CreateIngredientPayload
import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.ingredients.IngredientDeltaEntry
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import kotlin.uuid.Uuid

private const val ORDER_GAP = 10

fun createIngredients(
  recipeId: Uuid,
  request: List<CreateIngredientPayload>,
): List<Ingredient> =
  request.mapIndexed { index, ingredient ->
    val ingredientId = Uuid.random()
    val createdAt = Instant.now()
    val order = (index + 1) * ORDER_GAP

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
      it[IngredientDeltaTable.omitted] = false
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
  val rows =
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
        .toList()
    }

  val ingredientConceptCount =
    IngredientsTable
      .selectAll()
      .where { IngredientsTable.recipe_id eq recipeId }
      .count()

  if (rows.size.toLong() != ingredientConceptCount) {
    throw DataIntegrityException(
      "Missing ingredient_delta row for best_version on one or more ingredients of recipe $recipeId",
    )
  }

  // Omitted ingredients still have a best_version delta (the omission itself is versioned) — they're
  // just left out of the recipe's active ingredient list.
  return rows
    .filterNot { it[IngredientDeltaTable.omitted] }
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

fun insertIngredient(
  recipeId: Uuid,
  request: AddIngredientPayload,
): Ingredient =
  transaction {
    val order = resolveInsertionOrder(recipeId, request.previousIngredientId, request.nextIngredientId)

    val ingredientId = Uuid.random()
    val createdAt = Instant.now()

    IngredientsTable.insert {
      it[IngredientsTable.id] = ingredientId
      it[IngredientsTable.recipe_id] = recipeId
      it[IngredientsTable.best_version] = 1
      it[IngredientsTable.created_at] = createdAt
    }

    IngredientDeltaTable.insert {
      it[IngredientDeltaTable.ingredient_id] = ingredientId
      it[IngredientDeltaTable.version] = 1
      it[IngredientDeltaTable.amount] = request.amount
      it[IngredientDeltaTable.name] = request.name
      it[IngredientDeltaTable.notes] = request.notes
      it[IngredientDeltaTable.created_at] = createdAt
      it[IngredientDeltaTable.order] = order
      it[IngredientDeltaTable.omitted] = false
    }

    Ingredient(
      id = ingredientId,
      recipeId = recipeId,
      bestVersion = 1,
      notes = request.notes,
      createdAt = createdAt,
      amount = request.amount,
      name = request.name,
      order = order,
    )
  }

/**
 * Omits an ingredient by writing a new delta version carrying its current amount/name/notes/order
 * unchanged, but with `omitted = true`. The concept row and its history are never deleted — this
 * mirrors how every other ingredient edit already works, and leaves a clean path to "un-omit" later
 * by writing yet another delta with `omitted = false`.
 */
fun omitIngredientVersion(ingredientId: Uuid): Ingredient =
  transaction {
    val current = currentIngredientValues(ingredientId)

    applyIngredientDelta(
      ingredientId = ingredientId,
      amount = current.amount,
      name = current.name,
      notes = current.notes,
      order = current.order,
      omitted = true,
    )
  }

private fun resolveInsertionOrder(
  recipeId: Uuid,
  previousIngredientId: Uuid?,
  nextIngredientId: Uuid?,
): Int {
  val orders = activeIngredientOrders(recipeId)
  val previousOrder =
    previousIngredientId?.let { orders[it] ?: throw NotFoundException("Ingredient", it.toString()) }
  val nextOrder =
    nextIngredientId?.let { orders[it] ?: throw NotFoundException("Ingredient", it.toString()) }

  if (previousOrder != null && nextOrder != null && previousOrder >= nextOrder) {
    throw BadRequestException("previousIngredientId must currently come before nextIngredientId")
  }

  computeMidpointOrder(previousOrder, nextOrder)?.let { return it }

  // No integer room between the neighbors (e.g. orders 10 and 11) — renumber everything with
  // fresh gaps of ORDER_GAP and retry once against the new values.
  rebalanceIngredientOrders(recipeId)
  val refreshedOrders = activeIngredientOrders(recipeId)
  val refreshedPrevious = previousIngredientId?.let { refreshedOrders.getValue(it) }
  val refreshedNext = nextIngredientId?.let { refreshedOrders.getValue(it) }

  return computeMidpointOrder(refreshedPrevious, refreshedNext)
    ?: error("Unable to compute an insertion order for a new ingredient on recipe $recipeId")
}

private fun computeMidpointOrder(
  previousOrder: Int?,
  nextOrder: Int?,
): Int? =
  when {
    previousOrder == null && nextOrder == null -> ORDER_GAP
    previousOrder == null -> nextOrder!!.takeIf { it > 1 }?.div(2)
    nextOrder == null -> previousOrder + ORDER_GAP
    nextOrder - previousOrder >= 2 -> previousOrder + (nextOrder - previousOrder) / 2
    else -> null
  }

private fun activeIngredientOrders(recipeId: Uuid): Map<Uuid, Int> {
  val ingredientJoin =
    IngredientsTable.join(
      IngredientDeltaTable,
      JoinType.INNER,
      onColumn = IngredientsTable.id,
      otherColumn = IngredientDeltaTable.ingredient_id,
      additionalConstraint = { IngredientDeltaTable.version eq IngredientsTable.best_version },
    )

  return ingredientJoin
    .selectAll()
    .where { (IngredientsTable.recipe_id eq recipeId) and (IngredientDeltaTable.omitted eq false) }
    .associate { row -> row[IngredientsTable.id] to row[IngredientDeltaTable.order] }
}

private data class CurrentIngredientValues(
  val amount: String,
  val name: String,
  val notes: String?,
  val order: Int,
)

private fun currentIngredientValues(ingredientId: Uuid): CurrentIngredientValues {
  val ingredientJoin =
    IngredientsTable.join(
      IngredientDeltaTable,
      JoinType.INNER,
      onColumn = IngredientsTable.id,
      otherColumn = IngredientDeltaTable.ingredient_id,
      additionalConstraint = { IngredientDeltaTable.version eq IngredientsTable.best_version },
    )

  val row =
    ingredientJoin
      .selectAll()
      .where { IngredientsTable.id eq ingredientId }
      .singleOrNull() ?: throw NotFoundException("Ingredient", ingredientId.toString())

  return CurrentIngredientValues(
    amount = row[IngredientDeltaTable.amount],
    name = row[IngredientDeltaTable.name],
    notes = row[IngredientDeltaTable.notes],
    order = row[IngredientDeltaTable.order],
  )
}

private fun rebalanceIngredientOrders(recipeId: Uuid) {
  val ingredientJoin =
    IngredientsTable.join(
      IngredientDeltaTable,
      JoinType.INNER,
      onColumn = IngredientsTable.id,
      otherColumn = IngredientDeltaTable.ingredient_id,
      additionalConstraint = { IngredientDeltaTable.version eq IngredientsTable.best_version },
    )

  val active =
    ingredientJoin
      .selectAll()
      .where { (IngredientsTable.recipe_id eq recipeId) and (IngredientDeltaTable.omitted eq false) }
      .orderBy(IngredientDeltaTable.order to SortOrder.ASC)
      .map { row ->
        CurrentIngredientValues(
          amount = row[IngredientDeltaTable.amount],
          name = row[IngredientDeltaTable.name],
          notes = row[IngredientDeltaTable.notes],
          order = row[IngredientDeltaTable.order],
        ) to row[IngredientsTable.id]
      }

  active.forEachIndexed { index, (values, ingredientId) ->
    val newOrder = (index + 1) * ORDER_GAP
    if (newOrder != values.order) {
      applyIngredientDelta(
        ingredientId = ingredientId,
        amount = values.amount,
        name = values.name,
        notes = values.notes,
        order = newOrder,
        omitted = false,
      )
    }
  }
}

private fun applyIngredientDelta(
  ingredientId: Uuid,
  amount: String,
  name: String,
  notes: String?,
  order: Int,
  omitted: Boolean,
): Ingredient {
  val ingredientRow =
    IngredientsTable
      .selectAll()
      .where { IngredientsTable.id eq ingredientId }
      .singleOrNull() ?: throw NotFoundException("Ingredient", ingredientId.toString())

  val maxVersionExpr = IngredientDeltaTable.version.max()
  val highestVersion =
    IngredientDeltaTable
      .select(maxVersionExpr)
      .where { IngredientDeltaTable.ingredient_id eq ingredientId }
      .single()[maxVersionExpr] ?: 0

  val newVersion = highestVersion + 1

  IngredientDeltaTable.insert {
    it[IngredientDeltaTable.ingredient_id] = ingredientId
    it[IngredientDeltaTable.version] = newVersion
    it[IngredientDeltaTable.amount] = amount
    it[IngredientDeltaTable.name] = name
    it[IngredientDeltaTable.notes] = notes
    it[IngredientDeltaTable.created_at] = Instant.now()
    it[IngredientDeltaTable.order] = order
    it[IngredientDeltaTable.omitted] = omitted
  }

  IngredientsTable.update({ IngredientsTable.id eq ingredientId }) {
    it[IngredientsTable.best_version] = newVersion
  }

  return Ingredient(
    id = ingredientId,
    recipeId = ingredientRow[IngredientsTable.recipe_id],
    bestVersion = newVersion,
    notes = notes,
    createdAt = ingredientRow[IngredientsTable.created_at],
    amount = amount,
    name = name,
    order = order,
  )
}

data class BestIngredientDelta(
  val bakeIngredientId: Uuid,
  val bestDelta: IngredientDeltaEntry,
)
