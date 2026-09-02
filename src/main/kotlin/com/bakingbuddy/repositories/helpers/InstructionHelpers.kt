package com.bakingbuddy.repositories.helpers

import com.bakingbuddy.api.errors.DataIntegrityException
import com.bakingbuddy.database.InstructionDeltaTable
import com.bakingbuddy.database.InstructionsTable
import com.bakingbuddy.models.instructions.Instruction
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import kotlin.uuid.Uuid

fun createInstructions(
  recipeId: Uuid,
  request: List<String>,
): List<Instruction> =
  request.map { description ->
    val instructionId = Uuid.random()
    val createdAt = Instant.now()

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
    }

    Instruction(
      id = instructionId,
      recipeId = recipeId,
      bestVersion = 1,
      notes = null,
      createdAt = instructionStatement[InstructionsTable.created_at],
      description = description,
      order = null,
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

  val instructions =
    instructionJoin
      .selectAll()
      .where { InstructionsTable.recipe_id eq recipeId }
      .map { row ->
        Instruction(
          id = row[InstructionsTable.id],
          recipeId = row[InstructionsTable.recipe_id],
          bestVersion = row[InstructionsTable.best_version],
          notes = row[InstructionDeltaTable.notes],
          createdAt = row[InstructionsTable.created_at],
          description = row[InstructionDeltaTable.description],
          order = row[InstructionsTable.order],
        )
      }

  val instructionConceptCount =
    InstructionsTable
      .selectAll()
      .where { InstructionsTable.recipe_id eq recipeId }
      .count()

  if (instructions.size.toLong() != instructionConceptCount) {
    throw DataIntegrityException(
      "Missing instruction_delta row for best_version on one or more instructions of recipe $recipeId",
    )
  }

  return instructions
}

data class BestInstructionDelta(
  val deltaId: Uuid,
  val instructionId: Uuid,
  val bakeInstructionId: Uuid,
  val version: Int,
  val description: String,
  val notes: String?,
)
