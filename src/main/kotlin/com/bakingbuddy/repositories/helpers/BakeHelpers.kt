package com.bakingbuddy.repositories.helpers

import com.bakingbuddy.api.PatchField
import com.bakingbuddy.api.errors.ConflictException
import com.bakingbuddy.api.errors.DataIntegrityException
import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.database.BakeIngredientsTable
import com.bakingbuddy.database.BakeInstructionsTable
import com.bakingbuddy.database.BakeRatingsTable
import com.bakingbuddy.database.BakesTable
import com.bakingbuddy.database.IngredientDeltaTable
import com.bakingbuddy.database.IngredientsTable
import com.bakingbuddy.database.InstructionDeltaTable
import com.bakingbuddy.database.InstructionsTable
import com.bakingbuddy.models.bakes.BakeIngredient
import com.bakingbuddy.models.bakes.BakeIngredientPayload
import com.bakingbuddy.models.bakes.BakeInstruction
import com.bakingbuddy.models.bakes.BakeInstructionPayload
import com.bakingbuddy.models.bakes.BakeRating
import com.bakingbuddy.models.bakes.UpdateBakeRatingPayload
import com.bakingbuddy.models.ingredients.IngredientDeltaEntry
import com.bakingbuddy.models.instructions.InstructionDeltaEntry
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import kotlin.uuid.Uuid

// Steps added during a bake have no originating delta, so the delta tables are left-joined and their columns are
// only read when the bake row points at a delta.
fun getBakeInstructionPayloadForRow(row: ResultRow): BakeInstructionPayload {
  val initialDeltaId = row[BakeInstructionsTable.instruction_delta_id]

  return BakeInstructionPayload(
    bakeInstructionId = row[BakeInstructionsTable.id],
    initialDeltaValues =
      initialDeltaId?.let {
        InstructionDeltaEntry(
          id = it,
          instructionId = row[InstructionDeltaTable.instruction_id],
          version = row[InstructionDeltaTable.version],
          description = row[InstructionDeltaTable.description],
          notes = row[InstructionDeltaTable.notes],
          order = row[InstructionDeltaTable.order],
          createdAt = row[InstructionDeltaTable.created_at],
        )
      },
    updatedDeltaValues =
      BakeInstruction(
        updatedDescription = row[BakeInstructionsTable.description],
        updatedNotes = row[BakeInstructionsTable.notes],
        updatedOrder = row[BakeInstructionsTable.order],
        updatedOmitted = row[BakeInstructionsTable.omitted],
      ),
    completedBakeDeltaId = row[BakeInstructionsTable.completed_bake_delta_id],
  )
}

fun getBakeIngredientPayloadForRow(row: ResultRow): BakeIngredientPayload {
  val initialDeltaId = row[BakeIngredientsTable.ingredient_delta_id]

  return BakeIngredientPayload(
    bakeIngredientId = row[BakeIngredientsTable.id],
    initialDeltaValues =
      initialDeltaId?.let {
        IngredientDeltaEntry(
          ingredientId = row[IngredientDeltaTable.ingredient_id],
          id = it,
          version = row[IngredientDeltaTable.version],
          amount = row[IngredientDeltaTable.amount],
          name = row[IngredientDeltaTable.name],
          notes = row[IngredientDeltaTable.notes],
          createdAt = row[IngredientDeltaTable.created_at],
          order = row[IngredientDeltaTable.order],
        )
      },
    updatedDeltaValues =
      BakeIngredient(
        updatedAmount = row[BakeIngredientsTable.amount],
        updatedName = row[BakeIngredientsTable.name],
        updatedNotes = row[BakeIngredientsTable.notes],
        updatedOrder = row[BakeIngredientsTable.order],
        updatedOmitted = row[BakeIngredientsTable.omitted],
      ),
    completedBakeDeltaId = row[BakeIngredientsTable.completed_bake_delta_id],
  )
}

fun getBakeRatings(bakeIds: List<Uuid>): Map<Uuid, BakeRating> {
  val ratingsByBakeId =
    BakeRatingsTable
      .selectAll()
      .where { BakeRatingsTable.bake_id inList bakeIds }
      .associate { ratingRow ->
        ratingRow[BakeRatingsTable.bake_id] to
          BakeRating(
            overall = ratingRow[BakeRatingsTable.overall],
            taste = ratingRow[BakeRatingsTable.taste],
            texture = ratingRow[BakeRatingsTable.texture],
            appearance = ratingRow[BakeRatingsTable.appearance],
            riseStructure = ratingRow[BakeRatingsTable.rise_structure],
            difficulty = ratingRow[BakeRatingsTable.difficulty],
          )
      }
  return ratingsByBakeId
}

@Suppress("CyclomaticComplexMethod")
fun upsertBakeRatings(
  bakeId: Uuid,
  rating: PatchField<UpdateBakeRatingPayload>,
) {
  val payload =
    when (rating) {
      is PatchField.Absent -> return
      is PatchField.Present -> rating.value ?: return
    }

  val existingId =
    BakeRatingsTable
      .selectAll()
      .where { BakeRatingsTable.bake_id eq bakeId }
      .singleOrNull()
      ?.get(BakeRatingsTable.id)

  if (existingId == null) {
    BakeRatingsTable.insert {
      it[BakeRatingsTable.id] = Uuid.random()
      it[BakeRatingsTable.bake_id] = bakeId
      it[BakeRatingsTable.created_at] = Instant.now()
      when (val overall = payload.overall) {
        is PatchField.Absent -> {}
        is PatchField.Present -> it[BakeRatingsTable.overall] = overall.value
      }
      when (val taste = payload.taste) {
        is PatchField.Absent -> {}
        is PatchField.Present -> it[BakeRatingsTable.taste] = taste.value
      }
      when (val texture = payload.texture) {
        is PatchField.Absent -> {}
        is PatchField.Present -> it[BakeRatingsTable.texture] = texture.value
      }
      when (val appearance = payload.appearance) {
        is PatchField.Absent -> {}
        is PatchField.Present -> it[BakeRatingsTable.appearance] = appearance.value
      }
      when (val riseStructure = payload.riseStructure) {
        is PatchField.Absent -> {}
        is PatchField.Present -> it[BakeRatingsTable.rise_structure] = riseStructure.value
      }
      when (val difficulty = payload.difficulty) {
        is PatchField.Absent -> {}
        is PatchField.Present -> it[BakeRatingsTable.difficulty] = difficulty.value
      }
    }
  } else {
    BakeRatingsTable.update({ BakeRatingsTable.bake_id eq bakeId }) {
      when (val overall = payload.overall) {
        is PatchField.Absent -> {}
        is PatchField.Present -> it[BakeRatingsTable.overall] = overall.value
      }
      when (val taste = payload.taste) {
        is PatchField.Absent -> {}
        is PatchField.Present -> it[BakeRatingsTable.taste] = taste.value
      }
      when (val texture = payload.texture) {
        is PatchField.Absent -> {}
        is PatchField.Present -> it[BakeRatingsTable.texture] = texture.value
      }
      when (val appearance = payload.appearance) {
        is PatchField.Absent -> {}
        is PatchField.Present -> it[BakeRatingsTable.appearance] = appearance.value
      }
      when (val riseStructure = payload.riseStructure) {
        is PatchField.Absent -> {}
        is PatchField.Present -> it[BakeRatingsTable.rise_structure] = riseStructure.value
      }
      when (val difficulty = payload.difficulty) {
        is PatchField.Absent -> {}
        is PatchField.Present -> it[BakeRatingsTable.difficulty] = difficulty.value
      }
    }
  }
}

fun getBestIngredientDeltas(recipeId: Uuid): List<BestIngredientDelta> {
  val rows =
    IngredientsTable
      .join(
        IngredientDeltaTable,
        JoinType.INNER,
        onColumn = IngredientsTable.id,
        otherColumn = IngredientDeltaTable.ingredient_id,
        additionalConstraint = { IngredientDeltaTable.version eq IngredientsTable.best_version },
      ).selectAll()
      .where { IngredientsTable.recipe_id eq recipeId }
      .orderBy(IngredientDeltaTable.order to SortOrder.ASC)
      .toList()

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

  // Omitted ingredients still have a best_version delta, but a new bake must not start with them.
  return rows
    .filterNot { it[IngredientDeltaTable.omitted] }
    .map { row ->
      BestIngredientDelta(
        bakeIngredientId = Uuid.random(),
        bestDelta =
          IngredientDeltaEntry(
            id = row[IngredientDeltaTable.id],
            ingredientId = row[IngredientDeltaTable.ingredient_id],
            version = row[IngredientDeltaTable.version],
            amount = row[IngredientDeltaTable.amount],
            name = row[IngredientDeltaTable.name],
            notes = row[IngredientDeltaTable.notes],
            order = row[IngredientDeltaTable.order],
            createdAt = row[IngredientDeltaTable.created_at],
          ),
      )
    }
}

fun getBestInstructionDeltas(recipeId: Uuid): List<BestInstructionDelta> {
  val rows =
    InstructionsTable
      .join(
        InstructionDeltaTable,
        JoinType.INNER,
        onColumn = InstructionsTable.id,
        otherColumn = InstructionDeltaTable.instruction_id,
        additionalConstraint = { InstructionDeltaTable.version eq InstructionsTable.best_version },
      ).selectAll()
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

  // Omitted instructions still have a best_version delta, but a new bake must not start with them.
  return rows
    .filterNot { it[InstructionDeltaTable.omitted] }
    .map { row ->
      BestInstructionDelta(
        bakeInstructionId = Uuid.random(),
        bestDelta =
          InstructionDeltaEntry(
            id = row[InstructionDeltaTable.id],
            instructionId = row[InstructionDeltaTable.instruction_id],
            version = row[InstructionDeltaTable.version],
            description = row[InstructionDeltaTable.description],
            notes = row[InstructionDeltaTable.notes],
            order = row[InstructionDeltaTable.order],
            createdAt = row[InstructionDeltaTable.created_at],
          ),
      )
    }
}

/**
 * Carries a completed bake's ingredients over to the recipe.
 *
 * Every change is recorded as a new delta version tagged with the bake (`source_bake_id`) and linked from the bake
 * row (`completed_bake_delta_id`). When [setAsBest] is true the recipe is made to match the bake exactly, whatever
 * happened to the recipe while the bake was open: changed rows are compared against the recipe's *current* best
 * delta rather than the one the bake started from, and recipe ingredients the bake doesn't have are omitted.
 * When it is false, only the bake's own changes are recorded and `best_version` is left alone.
 */
fun completeBakeIngredients(
  bakeId: Uuid,
  recipeId: Uuid,
  setAsBest: Boolean,
  now: Instant,
) {
  val rows =
    BakeIngredientsTable
      .selectAll()
      .where { BakeIngredientsTable.bake_id eq bakeId }
      .toList()

  if (setAsBest) {
    omitIngredientsMissingFromBake(recipeId, bakeId, rows, now)
  }

  rows.forEach { row ->
    val initialDeltaId = row[BakeIngredientsTable.ingredient_delta_id]
    if (initialDeltaId == null) {
      recordBakeAddedIngredient(row, recipeId, setAsBest, now)
    } else {
      recordBakeIngredientChanges(row, initialDeltaId, setAsBest, now)
    }
  }
}

fun completeBakeInstructions(
  bakeId: Uuid,
  recipeId: Uuid,
  setAsBest: Boolean,
  now: Instant,
) {
  val rows =
    BakeInstructionsTable
      .selectAll()
      .where { BakeInstructionsTable.bake_id eq bakeId }
      .toList()

  if (setAsBest) {
    omitInstructionsMissingFromBake(recipeId, bakeId, rows, now)
  }

  rows.forEach { row ->
    val initialDeltaId = row[BakeInstructionsTable.instruction_delta_id]
    if (initialDeltaId == null) {
      recordBakeAddedInstruction(row, recipeId, setAsBest, now)
    } else {
      recordBakeInstructionChanges(row, initialDeltaId, setAsBest, now)
    }
  }
}

// Ingredients added to the recipe while the bake was open don't exist in the bake, so making the recipe match the
// bake means omitting them. (Versioned, so nothing is lost.)
private fun omitIngredientsMissingFromBake(
  recipeId: Uuid,
  bakeId: Uuid,
  bakeRows: List<ResultRow>,
  now: Instant,
) {
  val initialDeltaIds = bakeRows.mapNotNull { it[BakeIngredientsTable.ingredient_delta_id] }
  val representedIngredientIds: Set<Uuid> =
    if (initialDeltaIds.isEmpty()) {
      emptySet()
    } else {
      IngredientDeltaTable
        .select(IngredientDeltaTable.ingredient_id)
        .where { IngredientDeltaTable.id inList initialDeltaIds }
        .map { it[IngredientDeltaTable.ingredient_id] }
        .toSet()
    }

  getIngredientsForRecipe(recipeId)
    .filterNot { it.id in representedIngredientIds }
    .forEach { ingredient ->
      writeIngredientDelta(
        ingredientId = ingredient.id,
        amount = ingredient.amount,
        name = ingredient.name,
        notes = ingredient.notes,
        order = ingredient.order,
        omitted = true,
        sourceBakeId = bakeId,
        now = now,
      )
    }
}

private fun omitInstructionsMissingFromBake(
  recipeId: Uuid,
  bakeId: Uuid,
  bakeRows: List<ResultRow>,
  now: Instant,
) {
  val initialDeltaIds = bakeRows.mapNotNull { it[BakeInstructionsTable.instruction_delta_id] }
  val representedInstructionIds: Set<Uuid> =
    if (initialDeltaIds.isEmpty()) {
      emptySet()
    } else {
      InstructionDeltaTable
        .select(InstructionDeltaTable.instruction_id)
        .where { InstructionDeltaTable.id inList initialDeltaIds }
        .map { it[InstructionDeltaTable.instruction_id] }
        .toSet()
    }

  getInstructionsForRecipe(recipeId)
    .filterNot { it.id in representedInstructionIds }
    .forEach { instruction ->
      writeInstructionDelta(
        instructionId = instruction.id,
        description = instruction.description,
        notes = instruction.notes,
        order = instruction.order,
        omitted = true,
        sourceBakeId = bakeId,
        now = now,
      )
    }
}

// A bake ingredient that started from a recipe ingredient: edits, reorders, removals and reversions all show up as
// a difference between the bake row and the baseline.
private fun recordBakeIngredientChanges(
  row: ResultRow,
  initialDeltaId: Uuid,
  setAsBest: Boolean,
  now: Instant,
) {
  val initialDelta =
    IngredientDeltaTable
      .selectAll()
      .where { IngredientDeltaTable.id eq initialDeltaId }
      .singleOrNull() ?: throw NotFoundException("IngredientDelta", initialDeltaId.toString())
  val ingredientId = initialDelta[IngredientDeltaTable.ingredient_id]

  val bakeValues =
    CurrentIngredientValues(
      amount = row[BakeIngredientsTable.amount],
      name = row[BakeIngredientsTable.name],
      notes = row[BakeIngredientsTable.notes],
      order = row[BakeIngredientsTable.order],
      omitted = row[BakeIngredientsTable.omitted],
    )

  val baseline =
    if (setAsBest) {
      currentIngredientValues(ingredientId)
    } else {
      CurrentIngredientValues(
        amount = initialDelta[IngredientDeltaTable.amount],
        name = initialDelta[IngredientDeltaTable.name],
        notes = initialDelta[IngredientDeltaTable.notes],
        order = initialDelta[IngredientDeltaTable.order],
        omitted = initialDelta[IngredientDeltaTable.omitted],
      )
    }

  // Nothing to record if they match, or if the step is omitted on both sides (nothing visible differs).
  if (baseline == bakeValues || (baseline.omitted && bakeValues.omitted)) {
    return
  }

  val written =
    writeIngredientDelta(
      ingredientId = ingredientId,
      amount = bakeValues.amount,
      name = bakeValues.name,
      notes = bakeValues.notes,
      order = bakeValues.order,
      omitted = bakeValues.omitted,
      sourceBakeId = row[BakeIngredientsTable.bake_id],
      setAsBest = setAsBest,
      now = now,
    )

  BakeIngredientsTable.update({ BakeIngredientsTable.id eq row[BakeIngredientsTable.id] }) {
    it[BakeIngredientsTable.completed_bake_delta_id] = written.deltaId
  }
}

private fun recordBakeInstructionChanges(
  row: ResultRow,
  initialDeltaId: Uuid,
  setAsBest: Boolean,
  now: Instant,
) {
  val initialDelta =
    InstructionDeltaTable
      .selectAll()
      .where { InstructionDeltaTable.id eq initialDeltaId }
      .singleOrNull() ?: throw NotFoundException("InstructionDelta", initialDeltaId.toString())
  val instructionId = initialDelta[InstructionDeltaTable.instruction_id]

  val bakeValues =
    CurrentInstructionValues(
      description = row[BakeInstructionsTable.description],
      notes = row[BakeInstructionsTable.notes],
      order = row[BakeInstructionsTable.order],
      omitted = row[BakeInstructionsTable.omitted],
    )

  val baseline =
    if (setAsBest) {
      currentInstructionValues(instructionId)
    } else {
      CurrentInstructionValues(
        description = initialDelta[InstructionDeltaTable.description],
        notes = initialDelta[InstructionDeltaTable.notes],
        order = initialDelta[InstructionDeltaTable.order],
        omitted = initialDelta[InstructionDeltaTable.omitted],
      )
    }

  if (baseline == bakeValues || (baseline.omitted && bakeValues.omitted)) {
    return
  }

  val written =
    writeInstructionDelta(
      instructionId = instructionId,
      description = bakeValues.description,
      notes = bakeValues.notes,
      order = bakeValues.order,
      omitted = bakeValues.omitted,
      sourceBakeId = row[BakeInstructionsTable.bake_id],
      setAsBest = setAsBest,
      now = now,
    )

  BakeInstructionsTable.update({ BakeInstructionsTable.id eq row[BakeInstructionsTable.id] }) {
    it[BakeInstructionsTable.completed_bake_delta_id] = written.deltaId
  }
}

// A bake ingredient added during the bake becomes a new recipe ingredient. If the bake isn't marked best, version 1
// is written omitted: the ingredient and its source bake are on record, but the recipe itself doesn't change.
private fun recordBakeAddedIngredient(
  row: ResultRow,
  recipeId: Uuid,
  setAsBest: Boolean,
  now: Instant,
) {
  // Added and then removed within the same bake: nothing worth putting on the recipe.
  if (row[BakeIngredientsTable.omitted]) return

  val ingredientId = Uuid.random()
  val deltaId = Uuid.random()

  IngredientsTable.insert {
    it[IngredientsTable.id] = ingredientId
    it[IngredientsTable.recipe_id] = recipeId
    it[IngredientsTable.best_version] = 1
    it[IngredientsTable.created_at] = now
  }

  IngredientDeltaTable.insert {
    it[IngredientDeltaTable.id] = deltaId
    it[IngredientDeltaTable.ingredient_id] = ingredientId
    it[IngredientDeltaTable.version] = 1
    it[IngredientDeltaTable.amount] = row[BakeIngredientsTable.amount]
    it[IngredientDeltaTable.name] = row[BakeIngredientsTable.name]
    it[IngredientDeltaTable.notes] = row[BakeIngredientsTable.notes]
    it[IngredientDeltaTable.source_bake_id] = row[BakeIngredientsTable.bake_id]
    it[IngredientDeltaTable.created_at] = now
    it[IngredientDeltaTable.order] = row[BakeIngredientsTable.order]
    it[IngredientDeltaTable.omitted] = !setAsBest
  }

  BakeIngredientsTable.update({ BakeIngredientsTable.id eq row[BakeIngredientsTable.id] }) {
    it[BakeIngredientsTable.completed_bake_delta_id] = deltaId
  }
}

private fun recordBakeAddedInstruction(
  row: ResultRow,
  recipeId: Uuid,
  setAsBest: Boolean,
  now: Instant,
) {
  if (row[BakeInstructionsTable.omitted]) return

  val instructionId = Uuid.random()
  val deltaId = Uuid.random()

  InstructionsTable.insert {
    it[InstructionsTable.id] = instructionId
    it[InstructionsTable.recipe_id] = recipeId
    it[InstructionsTable.best_version] = 1
    it[InstructionsTable.created_at] = now
  }

  InstructionDeltaTable.insert {
    it[InstructionDeltaTable.id] = deltaId
    it[InstructionDeltaTable.instruction_id] = instructionId
    it[InstructionDeltaTable.version] = 1
    it[InstructionDeltaTable.description] = row[BakeInstructionsTable.description]
    it[InstructionDeltaTable.notes] = row[BakeInstructionsTable.notes]
    it[InstructionDeltaTable.source_bake_id] = row[BakeInstructionsTable.bake_id]
    it[InstructionDeltaTable.created_at] = now
    it[InstructionDeltaTable.order] = row[BakeInstructionsTable.order]
    it[InstructionDeltaTable.omitted] = !setAsBest
  }

  BakeInstructionsTable.update({ BakeInstructionsTable.id eq row[BakeInstructionsTable.id] }) {
    it[BakeInstructionsTable.completed_bake_delta_id] = deltaId
  }
}

fun assertNoOpenBake(recipeId: Uuid) {
  val openBakeExists =
    BakesTable
      .selectAll()
      .where {
        (BakesTable.recipe_id eq recipeId) and
          (BakesTable.end_datetime.isNull())
      }.limit(1)
      .any()

  if (openBakeExists) {
    throw ConflictException("existingOpenBake")
  }
}
