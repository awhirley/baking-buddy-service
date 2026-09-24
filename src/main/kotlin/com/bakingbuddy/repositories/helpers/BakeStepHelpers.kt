package com.bakingbuddy.repositories.helpers

import com.bakingbuddy.api.errors.ConflictException
import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.database.BakeIngredientsTable
import com.bakingbuddy.database.BakeInstructionsTable
import com.bakingbuddy.database.BakesTable
import com.bakingbuddy.models.bakes.AddBakeIngredientPayload
import com.bakingbuddy.models.bakes.AddBakeInstructionPayload
import com.bakingbuddy.models.bakes.BakeIngredient
import com.bakingbuddy.models.bakes.BakeIngredientPayload
import com.bakingbuddy.models.bakes.BakeInstruction
import com.bakingbuddy.models.bakes.BakeInstructionPayload
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.Uuid

/**
 * Adds an ingredient to an open bake. Neighbour ids are bake ingredient ids and are resolved against the bake's
 * own active rows, so the frontend positions steps exactly as it does on a recipe. The row has no originating
 * delta (`ingredient_delta_id` is null); it is turned into a recipe ingredient when the bake completes.
 */
fun insertBakeIngredient(
  bakeId: Uuid,
  request: AddBakeIngredientPayload,
): BakeIngredientPayload =
  transaction {
    requireOpenBake(bakeId)

    val order =
      resolveInsertionOrder(
        entityLabel = "Bake ingredient",
        previousParam = "previousBakeIngredientId",
        nextParam = "nextBakeIngredientId",
        previousId = request.previousBakeIngredientId,
        nextId = request.nextBakeIngredientId,
        activeOrders = { activeBakeIngredientOrders(bakeId) },
        rebalance = { rebalanceBakeIngredientOrders(bakeId) },
      )

    val bakeIngredientId = Uuid.random()

    BakeIngredientsTable.insert {
      it[BakeIngredientsTable.id] = bakeIngredientId
      it[BakeIngredientsTable.bake_id] = bakeId
      it[BakeIngredientsTable.amount] = request.amount
      it[BakeIngredientsTable.name] = request.name
      it[BakeIngredientsTable.notes] = request.notes
      it[BakeIngredientsTable.order] = order
      it[BakeIngredientsTable.omitted] = false
    }

    BakeIngredientPayload(
      bakeIngredientId = bakeIngredientId,
      initialDeltaValues = null,
      updatedDeltaValues =
        BakeIngredient(
          updatedAmount = request.amount,
          updatedName = request.name,
          updatedNotes = request.notes,
          updatedOrder = order,
          updatedOmitted = false,
        ),
      completedBakeDeltaId = null,
    )
  }

/** Removes an ingredient from an open bake by flagging the row; the row stays so the bake's history is intact. */
fun markBakeIngredientOmitted(
  bakeId: Uuid,
  bakeIngredientId: Uuid,
) {
  transaction {
    requireOpenBake(bakeId)

    BakeIngredientsTable
      .selectAll()
      .where { (BakeIngredientsTable.id eq bakeIngredientId) and (BakeIngredientsTable.bake_id eq bakeId) }
      .singleOrNull()
      ?: throw NotFoundException("Bake ingredient", "bakeId=$bakeId, bakeIngredientId=$bakeIngredientId")

    BakeIngredientsTable.update({ BakeIngredientsTable.id eq bakeIngredientId }) {
      it[BakeIngredientsTable.omitted] = true
    }
  }
}

/** Adds an instruction to an open bake; see [insertBakeIngredient]. */
fun insertBakeInstruction(
  bakeId: Uuid,
  request: AddBakeInstructionPayload,
): BakeInstructionPayload =
  transaction {
    requireOpenBake(bakeId)

    val order =
      resolveInsertionOrder(
        entityLabel = "Bake instruction",
        previousParam = "previousBakeInstructionId",
        nextParam = "nextBakeInstructionId",
        previousId = request.previousBakeInstructionId,
        nextId = request.nextBakeInstructionId,
        activeOrders = { activeBakeInstructionOrders(bakeId) },
        rebalance = { rebalanceBakeInstructionOrders(bakeId) },
      )

    val bakeInstructionId = Uuid.random()

    BakeInstructionsTable.insert {
      it[BakeInstructionsTable.id] = bakeInstructionId
      it[BakeInstructionsTable.bake_id] = bakeId
      it[BakeInstructionsTable.description] = request.description
      it[BakeInstructionsTable.notes] = request.notes
      it[BakeInstructionsTable.order] = order
      it[BakeInstructionsTable.omitted] = false
    }

    BakeInstructionPayload(
      bakeInstructionId = bakeInstructionId,
      initialDeltaValues = null,
      updatedDeltaValues =
        BakeInstruction(
          updatedDescription = request.description,
          updatedNotes = request.notes,
          updatedOrder = order,
          updatedOmitted = false,
        ),
      completedBakeDeltaId = null,
    )
  }

/** Removes an instruction from an open bake by flagging the row; see [markBakeIngredientOmitted]. */
fun markBakeInstructionOmitted(
  bakeId: Uuid,
  bakeInstructionId: Uuid,
) {
  transaction {
    requireOpenBake(bakeId)

    BakeInstructionsTable
      .selectAll()
      .where { (BakeInstructionsTable.id eq bakeInstructionId) and (BakeInstructionsTable.bake_id eq bakeId) }
      .singleOrNull()
      ?: throw NotFoundException("Bake instruction", "bakeId=$bakeId, bakeInstructionId=$bakeInstructionId")

    BakeInstructionsTable.update({ BakeInstructionsTable.id eq bakeInstructionId }) {
      it[BakeInstructionsTable.omitted] = true
    }
  }
}

private fun requireOpenBake(bakeId: Uuid) {
  val bakeRow =
    BakesTable
      .selectAll()
      .where { BakesTable.id eq bakeId }
      .singleOrNull() ?: throw NotFoundException("Bake", bakeId.toString())

  if (bakeRow[BakesTable.end_datetime] != null) {
    throw ConflictException("bakeAlreadyComplete")
  }
}

private fun activeBakeIngredientOrders(bakeId: Uuid): Map<Uuid, Int> =
  BakeIngredientsTable
    .selectAll()
    .where { (BakeIngredientsTable.bake_id eq bakeId) and (BakeIngredientsTable.omitted eq false) }
    .associate { row -> row[BakeIngredientsTable.id] to row[BakeIngredientsTable.order] }

private fun activeBakeInstructionOrders(bakeId: Uuid): Map<Uuid, Int> =
  BakeInstructionsTable
    .selectAll()
    .where { (BakeInstructionsTable.bake_id eq bakeId) and (BakeInstructionsTable.omitted eq false) }
    .associate { row -> row[BakeInstructionsTable.id] to row[BakeInstructionsTable.order] }

// Bake rows are plain rows (no versioning), so rebalancing just renumbers them in place.
private fun rebalanceBakeIngredientOrders(bakeId: Uuid) {
  BakeIngredientsTable
    .selectAll()
    .where { (BakeIngredientsTable.bake_id eq bakeId) and (BakeIngredientsTable.omitted eq false) }
    .orderBy(BakeIngredientsTable.order to SortOrder.ASC)
    .map { row -> row[BakeIngredientsTable.id] to row[BakeIngredientsTable.order] }
    .forEachIndexed { index, (bakeIngredientId, currentOrder) ->
      val newOrder = (index + 1) * ORDER_GAP
      if (newOrder != currentOrder) {
        BakeIngredientsTable.update({ BakeIngredientsTable.id eq bakeIngredientId }) {
          it[BakeIngredientsTable.order] = newOrder
        }
      }
    }
}

private fun rebalanceBakeInstructionOrders(bakeId: Uuid) {
  BakeInstructionsTable
    .selectAll()
    .where { (BakeInstructionsTable.bake_id eq bakeId) and (BakeInstructionsTable.omitted eq false) }
    .orderBy(BakeInstructionsTable.order to SortOrder.ASC)
    .map { row -> row[BakeInstructionsTable.id] to row[BakeInstructionsTable.order] }
    .forEachIndexed { index, (bakeInstructionId, currentOrder) ->
      val newOrder = (index + 1) * ORDER_GAP
      if (newOrder != currentOrder) {
        BakeInstructionsTable.update({ BakeInstructionsTable.id eq bakeInstructionId }) {
          it[BakeInstructionsTable.order] = newOrder
        }
      }
    }
}
