package com.bakingbuddy.repositories.helpers

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.DataIntegrityException
import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.database.InstructionDeltaTable
import com.bakingbuddy.database.InstructionsTable
import com.bakingbuddy.models.instructions.AddInstructionPayload
import com.bakingbuddy.models.instructions.Instruction
import com.bakingbuddy.models.instructions.InstructionDeltaEntry
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

fun createInstructions(
  recipeId: Uuid,
  request: List<String>,
): List<Instruction> =
  request.mapIndexed { index, description ->
    val instructionId = Uuid.random()
    val createdAt = Instant.now()
    val order = (index + 1) * ORDER_GAP

    val instructionStatement =
      InstructionsTable.insert {
        it[InstructionsTable.id] = instructionId
        it[InstructionsTable.recipe_id] = recipeId
        it[InstructionsTable.best_version] = 1
        it[InstructionsTable.created_at] = createdAt
      }

    InstructionDeltaTable.insert {
      it[InstructionDeltaTable.instruction_id] = instructionId
      it[InstructionDeltaTable.version] = 1
      it[InstructionDeltaTable.description] = description
      it[InstructionDeltaTable.created_at] = createdAt
      it[InstructionDeltaTable.order] = order
      it[InstructionDeltaTable.omitted] = false
    }

    Instruction(
      id = instructionId,
      recipeId = recipeId,
      bestVersion = 1,
      notes = null,
      createdAt = instructionStatement[InstructionsTable.created_at],
      description = description,
      order = order,
    )
  }

fun getInstructionsForRecipe(recipeId: Uuid): List<Instruction> {
  val instructionJoin =
    InstructionsTable.join(
      InstructionDeltaTable,
      JoinType.INNER,
      onColumn = InstructionsTable.id,
      otherColumn = InstructionDeltaTable.instruction_id,
      additionalConstraint = { InstructionDeltaTable.version eq InstructionsTable.best_version },
    )

  val rows =
    instructionJoin
      .selectAll()
      .where { InstructionsTable.recipe_id eq recipeId }
      .orderBy(InstructionDeltaTable.order to SortOrder.ASC)
      .toList()

  val instructionConceptCount =
    InstructionsTable
      .selectAll()
      .where { InstructionsTable.recipe_id eq recipeId }
      .count()

  if (rows.size.toLong() != instructionConceptCount) {
    throw DataIntegrityException(
      "Missing instruction_delta row for best_version on one or more instructions of recipe $recipeId",
    )
  }

  // Omitted instructions still have a best_version delta (the omission itself is versioned) — they're
  // just left out of the recipe's active instruction list.
  return rows
    .filterNot { it[InstructionDeltaTable.omitted] }
    .map { row ->
      Instruction(
        id = row[InstructionsTable.id],
        recipeId = row[InstructionsTable.recipe_id],
        bestVersion = row[InstructionsTable.best_version],
        notes = row[InstructionDeltaTable.notes],
        createdAt = row[InstructionsTable.created_at],
        description = row[InstructionDeltaTable.description],
        order = row[InstructionDeltaTable.order],
      )
    }
}

fun insertInstruction(
  recipeId: Uuid,
  request: AddInstructionPayload,
): Instruction =
  transaction {
    val order = resolveInsertionOrder(recipeId, request.previousInstructionId, request.nextInstructionId)

    val instructionId = Uuid.random()
    val createdAt = Instant.now()

    InstructionsTable.insert {
      it[InstructionsTable.id] = instructionId
      it[InstructionsTable.recipe_id] = recipeId
      it[InstructionsTable.best_version] = 1
      it[InstructionsTable.created_at] = createdAt
    }

    InstructionDeltaTable.insert {
      it[InstructionDeltaTable.instruction_id] = instructionId
      it[InstructionDeltaTable.version] = 1
      it[InstructionDeltaTable.description] = request.description
      it[InstructionDeltaTable.notes] = request.notes
      it[InstructionDeltaTable.created_at] = createdAt
      it[InstructionDeltaTable.order] = order
      it[InstructionDeltaTable.omitted] = false
    }

    Instruction(
      id = instructionId,
      recipeId = recipeId,
      bestVersion = 1,
      notes = request.notes,
      createdAt = createdAt,
      description = request.description,
      order = order,
    )
  }

/**
 * Omits an instruction by writing a new delta version carrying its current description/notes/order
 * unchanged, but with `omitted = true`. The concept row and its history are never deleted — this
 * mirrors how every other instruction edit already works, and leaves a clean path to "un-omit" later
 * by writing yet another delta with `omitted = false`.
 */
fun omitInstructionVersion(instructionId: Uuid): Instruction =
  transaction {
    val current = currentInstructionValues(instructionId)

    applyInstructionDelta(
      instructionId = instructionId,
      description = current.description,
      notes = current.notes,
      order = current.order,
      omitted = true,
    )
  }

private fun resolveInsertionOrder(
  recipeId: Uuid,
  previousInstructionId: Uuid?,
  nextInstructionId: Uuid?,
): Int {
  val orders = activeInstructionOrders(recipeId)
  val previousOrder =
    previousInstructionId?.let { orders[it] ?: throw NotFoundException("Instruction", it.toString()) }
  val nextOrder =
    nextInstructionId?.let { orders[it] ?: throw NotFoundException("Instruction", it.toString()) }

  if (previousOrder != null && nextOrder != null && previousOrder >= nextOrder) {
    throw BadRequestException("previousInstructionId must currently come before nextInstructionId")
  }

  computeMidpointOrder(previousOrder, nextOrder)?.let { return it }

  // No integer room between the neighbors (e.g. orders 10 and 11) — renumber everything with
  // fresh gaps of ORDER_GAP and retry once against the new values.
  rebalanceInstructionOrders(recipeId)
  val refreshedOrders = activeInstructionOrders(recipeId)
  val refreshedPrevious = previousInstructionId?.let { refreshedOrders.getValue(it) }
  val refreshedNext = nextInstructionId?.let { refreshedOrders.getValue(it) }

  return computeMidpointOrder(refreshedPrevious, refreshedNext)
    ?: error("Unable to compute an insertion order for a new instruction on recipe $recipeId")
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

private fun activeInstructionOrders(recipeId: Uuid): Map<Uuid, Int> {
  val instructionJoin =
    InstructionsTable.join(
      InstructionDeltaTable,
      JoinType.INNER,
      onColumn = InstructionsTable.id,
      otherColumn = InstructionDeltaTable.instruction_id,
      additionalConstraint = { InstructionDeltaTable.version eq InstructionsTable.best_version },
    )

  return instructionJoin
    .selectAll()
    .where { (InstructionsTable.recipe_id eq recipeId) and (InstructionDeltaTable.omitted eq false) }
    .associate { row -> row[InstructionsTable.id] to row[InstructionDeltaTable.order] }
}

private data class CurrentInstructionValues(
  val description: String,
  val notes: String?,
  val order: Int,
)

private fun currentInstructionValues(instructionId: Uuid): CurrentInstructionValues {
  val instructionJoin =
    InstructionsTable.join(
      InstructionDeltaTable,
      JoinType.INNER,
      onColumn = InstructionsTable.id,
      otherColumn = InstructionDeltaTable.instruction_id,
      additionalConstraint = { InstructionDeltaTable.version eq InstructionsTable.best_version },
    )

  val row =
    instructionJoin
      .selectAll()
      .where { InstructionsTable.id eq instructionId }
      .singleOrNull() ?: throw NotFoundException("Instruction", instructionId.toString())

  return CurrentInstructionValues(
    description = row[InstructionDeltaTable.description],
    notes = row[InstructionDeltaTable.notes],
    order = row[InstructionDeltaTable.order],
  )
}

private fun rebalanceInstructionOrders(recipeId: Uuid) {
  val instructionJoin =
    InstructionsTable.join(
      InstructionDeltaTable,
      JoinType.INNER,
      onColumn = InstructionsTable.id,
      otherColumn = InstructionDeltaTable.instruction_id,
      additionalConstraint = { InstructionDeltaTable.version eq InstructionsTable.best_version },
    )

  val active =
    instructionJoin
      .selectAll()
      .where { (InstructionsTable.recipe_id eq recipeId) and (InstructionDeltaTable.omitted eq false) }
      .orderBy(InstructionDeltaTable.order to SortOrder.ASC)
      .map { row ->
        CurrentInstructionValues(
          description = row[InstructionDeltaTable.description],
          notes = row[InstructionDeltaTable.notes],
          order = row[InstructionDeltaTable.order],
        ) to row[InstructionsTable.id]
      }

  active.forEachIndexed { index, (values, instructionId) ->
    val newOrder = (index + 1) * ORDER_GAP
    if (newOrder != values.order) {
      applyInstructionDelta(
        instructionId = instructionId,
        description = values.description,
        notes = values.notes,
        order = newOrder,
        omitted = false,
      )
    }
  }
}

private fun applyInstructionDelta(
  instructionId: Uuid,
  description: String,
  notes: String?,
  order: Int,
  omitted: Boolean,
): Instruction {
  val instructionRow =
    InstructionsTable
      .selectAll()
      .where { InstructionsTable.id eq instructionId }
      .singleOrNull() ?: throw NotFoundException("Instruction", instructionId.toString())

  val maxVersionExpr = InstructionDeltaTable.version.max()
  val highestVersion =
    InstructionDeltaTable
      .select(maxVersionExpr)
      .where { InstructionDeltaTable.instruction_id eq instructionId }
      .single()[maxVersionExpr] ?: 0

  val newVersion = highestVersion + 1

  InstructionDeltaTable.insert {
    it[InstructionDeltaTable.instruction_id] = instructionId
    it[InstructionDeltaTable.version] = newVersion
    it[InstructionDeltaTable.description] = description
    it[InstructionDeltaTable.notes] = notes
    it[InstructionDeltaTable.created_at] = Instant.now()
    it[InstructionDeltaTable.order] = order
    it[InstructionDeltaTable.omitted] = omitted
  }

  InstructionsTable.update({ InstructionsTable.id eq instructionId }) {
    it[InstructionsTable.best_version] = newVersion
  }

  return Instruction(
    id = instructionId,
    recipeId = instructionRow[InstructionsTable.recipe_id],
    bestVersion = newVersion,
    notes = notes,
    createdAt = instructionRow[InstructionsTable.created_at],
    description = description,
    order = order,
  )
}

data class BestInstructionDelta(
  val bakeInstructionId: Uuid,
  val bestDelta: InstructionDeltaEntry,
)
