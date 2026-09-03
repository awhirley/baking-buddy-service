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
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import kotlin.uuid.Uuid

fun getBakeInstructionPayloadForRow(row: ResultRow): BakeInstructionPayload =
  BakeInstructionPayload(
    bakeInstructionId = row[BakeInstructionsTable.id],
    initialDeltaValues =
      InstructionDeltaEntry(
        id = row[BakeInstructionsTable.instruction_delta_id],
        instructionId = row[InstructionDeltaTable.instruction_id],
        version = row[InstructionDeltaTable.version],
        description = row[InstructionDeltaTable.description],
        notes = row[InstructionDeltaTable.notes],
        order = row[InstructionDeltaTable.order],
        createdAt = row[InstructionDeltaTable.created_at],
      ),
    updatedDeltaValues =
      BakeInstruction(
        updatedDescription = row[BakeInstructionsTable.description],
        updatedNotes = row[BakeInstructionsTable.notes],
        updatedOrder = row[BakeInstructionsTable.order],
      ),
    completedBakeDeltaId = row[BakeInstructionsTable.completed_bake_delta_id],
  )

fun getBakeIngredientPayloadForRow(row: ResultRow): BakeIngredientPayload =
  BakeIngredientPayload(
    bakeIngredientId = row[BakeIngredientsTable.id],
    initialDeltaValues =
      IngredientDeltaEntry(
        ingredientId = row[IngredientDeltaTable.ingredient_id],
        id = row[BakeIngredientsTable.ingredient_delta_id],
        version = row[IngredientDeltaTable.version],
        amount = row[IngredientDeltaTable.amount],
        name = row[IngredientDeltaTable.name],
        notes = row[IngredientDeltaTable.notes],
        createdAt = row[IngredientDeltaTable.created_at],
        order = row[BakeIngredientsTable.order],
      ),
    updatedDeltaValues =
      BakeIngredient(
        updatedAmount = row[BakeIngredientsTable.amount],
        updatedName = row[BakeIngredientsTable.name],
        updatedNotes = row[BakeIngredientsTable.notes],
        updatedOrder = row[BakeIngredientsTable.order],
      ),
    completedBakeDeltaId = row[BakeIngredientsTable.completed_bake_delta_id],
  )

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
  val ingredientDeltas =
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

  val ingredientConceptCount =
    IngredientsTable
      .selectAll()
      .where { IngredientsTable.recipe_id eq recipeId }
      .count()

  if (ingredientDeltas.size.toLong() != ingredientConceptCount) {
    throw DataIntegrityException(
      "Missing ingredient_delta row for best_version on one or more ingredients of recipe $recipeId",
    )
  }

  return ingredientDeltas
}

fun getBestInstructionDeltas(recipeId: Uuid): List<BestInstructionDelta> {
  val instructionDeltas =
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

  val instructionConceptCount =
    InstructionsTable
      .selectAll()
      .where { InstructionsTable.recipe_id eq recipeId }
      .count()

  if (instructionDeltas.size.toLong() != instructionConceptCount) {
    throw DataIntegrityException(
      "Missing instruction_delta row for best_version on one or more instructions of recipe $recipeId",
    )
  }

  return instructionDeltas
}

fun completeBakeIngredient(
  row: ResultRow,
  setAsBest: Boolean,
  now: Instant,
) {
  val amount = row[BakeIngredientsTable.amount]
  val name = row[BakeIngredientsTable.name]
  val notes = row[BakeIngredientsTable.notes]

  val currentDeltaId = row[BakeIngredientsTable.ingredient_delta_id]
  val currentDelta =
    IngredientDeltaTable
      .selectAll()
      .where { IngredientDeltaTable.id eq currentDeltaId }
      .singleOrNull() ?: throw NotFoundException("IngredientDelta", currentDeltaId.toString())
  val ingredientId = currentDelta[IngredientDeltaTable.ingredient_id]

  val newVersion = nextIngredientDeltaVersion(ingredientId)
  val newDeltaId = Uuid.random()

  IngredientDeltaTable.insert {
    it[IngredientDeltaTable.id] = newDeltaId
    it[IngredientDeltaTable.ingredient_id] = ingredientId
    it[IngredientDeltaTable.version] = newVersion
    it[IngredientDeltaTable.amount] = amount
    it[IngredientDeltaTable.name] = name
    it[IngredientDeltaTable.notes] = notes
    it[IngredientDeltaTable.created_at] = now
    it[IngredientDeltaTable.source_bake_id] = row[BakeIngredientsTable.bake_id]
  }

  BakeIngredientsTable.update({ BakeIngredientsTable.id eq row[BakeIngredientsTable.id] }) {
    it[BakeIngredientsTable.completed_bake_delta_id] = newDeltaId
  }

  if (setAsBest) {
    IngredientsTable.update({ IngredientsTable.id eq ingredientId }) {
      it[IngredientsTable.best_version] = newVersion
    }
  }
}

fun completeBakeInstruction(
  row: ResultRow,
  setAsBest: Boolean,
  now: Instant,
) {
  val description = row[BakeInstructionsTable.description]
  val notes = row[BakeInstructionsTable.notes]

  val currentDeltaId = row[BakeInstructionsTable.instruction_delta_id]
  val currentDelta =
    InstructionDeltaTable
      .selectAll()
      .where { InstructionDeltaTable.id eq currentDeltaId }
      .singleOrNull() ?: throw NotFoundException("InstructionDelta", currentDeltaId.toString())
  val instructionId = currentDelta[InstructionDeltaTable.instruction_id]

  val newVersion = nextInstructionDeltaVersion(instructionId)
  val newDeltaId = Uuid.random()

  InstructionDeltaTable.insert {
    it[InstructionDeltaTable.id] = newDeltaId
    it[InstructionDeltaTable.instruction_id] = instructionId
    it[InstructionDeltaTable.version] = newVersion
    it[InstructionDeltaTable.description] = description
    it[InstructionDeltaTable.notes] = notes
    it[InstructionDeltaTable.created_at] = now
    it[InstructionDeltaTable.source_bake_id] = row[BakeInstructionsTable.bake_id]
  }

  BakeInstructionsTable.update({ BakeInstructionsTable.id eq row[BakeInstructionsTable.id] }) {
    it[BakeInstructionsTable.completed_bake_delta_id] = newDeltaId
  }

  if (setAsBest) {
    InstructionsTable.update({ InstructionsTable.id eq instructionId }) {
      it[InstructionsTable.best_version] = newVersion
    }
  }
}

fun nextIngredientDeltaVersion(ingredientId: Uuid): Int {
  val maxVersionExpr = IngredientDeltaTable.version.max()
  val highestVersion =
    IngredientDeltaTable
      .select(maxVersionExpr)
      .where { IngredientDeltaTable.ingredient_id eq ingredientId }
      .single()[maxVersionExpr] ?: 0

  return highestVersion + 1
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
