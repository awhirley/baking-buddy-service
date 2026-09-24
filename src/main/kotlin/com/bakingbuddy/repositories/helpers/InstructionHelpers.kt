package com.bakingbuddy.repositories.helpers

import com.bakingbuddy.api.errors.DataIntegrityException
import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.database.InstructionDeltaTable
import com.bakingbuddy.database.InstructionsTable
import com.bakingbuddy.models.instructions.AddInstructionPayload
import com.bakingbuddy.models.instructions.Instruction
import com.bakingbuddy.models.instructions.InstructionDeltaEntry
import com.bakingbuddy.models.instructions.UpdateInstructionPayload
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
    val order =
      resolveInsertionOrder(
        entityLabel = "Instruction",
        previousParam = "previousInstructionId",
        nextParam = "nextInstructionId",
        previousId = request.previousInstructionId,
        nextId = request.nextInstructionId,
        activeOrders = { activeInstructionOrders(recipeId) },
        rebalance = { rebalanceInstructionOrders(recipeId) },
      )

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

data class CurrentInstructionValues(
  val description: String,
  val notes: String?,
  val order: Int,
  val omitted: Boolean,
)

fun currentInstructionValues(instructionId: Uuid): CurrentInstructionValues {
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
    omitted = row[InstructionDeltaTable.omitted],
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
          omitted = row[InstructionDeltaTable.omitted],
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

/**
 * Appends a new delta version for an instruction and, unless [setAsBest] is false, points `best_version` at it.
 * [sourceBakeId] records which bake the change came from (null for edits made directly on the recipe).
 */
fun writeInstructionDelta(
  instructionId: Uuid,
  description: String,
  notes: String?,
  order: Int,
  omitted: Boolean,
  sourceBakeId: Uuid? = null,
  setAsBest: Boolean = true,
  now: Instant = Instant.now(),
): WrittenDelta {
  val newVersion = nextInstructionDeltaVersion(instructionId)
  val deltaId = Uuid.random()

  InstructionDeltaTable.insert {
    it[InstructionDeltaTable.id] = deltaId
    it[InstructionDeltaTable.instruction_id] = instructionId
    it[InstructionDeltaTable.version] = newVersion
    it[InstructionDeltaTable.description] = description
    it[InstructionDeltaTable.notes] = notes
    it[InstructionDeltaTable.source_bake_id] = sourceBakeId
    it[InstructionDeltaTable.created_at] = now
    it[InstructionDeltaTable.order] = order
    it[InstructionDeltaTable.omitted] = omitted
  }

  if (setAsBest) {
    InstructionsTable.update({ InstructionsTable.id eq instructionId }) {
      it[InstructionsTable.best_version] = newVersion
    }
  }

  return WrittenDelta(deltaId, newVersion)
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

  val written =
    writeInstructionDelta(
      instructionId = instructionId,
      description = description,
      notes = notes,
      order = order,
      omitted = omitted,
    )

  return Instruction(
    id = instructionId,
    recipeId = instructionRow[InstructionsTable.recipe_id],
    bestVersion = written.version,
    notes = notes,
    createdAt = instructionRow[InstructionsTable.created_at],
    description = description,
    order = order,
  )
}

/**
 * Edits an instruction on the recipe by writing a new delta version. The current `omitted` state is carried forward, so
 * editing never brings a removed instruction back.
 */
fun updateInstructionVersion(
  instructionId: Uuid,
  request: UpdateInstructionPayload,
): Instruction =
  transaction {
    val current = currentInstructionValues(instructionId)

    applyInstructionDelta(
      instructionId = instructionId,
      description = request.description,
      notes = request.notes,
      order = request.order,
      omitted = current.omitted,
    )
  }

fun nextInstructionDeltaVersion(instructionId: Uuid): Int {
  val maxVersionExpr = InstructionDeltaTable.version.max()
  val highestVersion =
    InstructionDeltaTable
      .select(maxVersionExpr)
      .where { InstructionDeltaTable.instruction_id eq instructionId }
      .single()[maxVersionExpr] ?: 0

  return highestVersion + 1
}

data class BestInstructionDelta(
  val bakeInstructionId: Uuid,
  val bestDelta: InstructionDeltaEntry,
)
