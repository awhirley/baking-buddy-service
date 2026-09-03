package com.bakingbuddy.repositories

import com.bakingbuddy.api.PatchField
import com.bakingbuddy.api.errors.ConflictException
import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.database.BakeIngredientsTable
import com.bakingbuddy.database.BakeInstructionsTable
import com.bakingbuddy.database.BakesTable
import com.bakingbuddy.database.IngredientDeltaTable
import com.bakingbuddy.database.InstructionDeltaTable
import com.bakingbuddy.database.RecipesTable
import com.bakingbuddy.models.bakes.Bake
import com.bakingbuddy.models.bakes.BakeDetail
import com.bakingbuddy.models.bakes.BakeIngredient
import com.bakingbuddy.models.bakes.BakeIngredientPayload
import com.bakingbuddy.models.bakes.BakeInstruction
import com.bakingbuddy.models.bakes.BakeInstructionPayload
import com.bakingbuddy.models.bakes.CompleteBakePayload
import com.bakingbuddy.models.bakes.UpdateBakeIngredientPayload
import com.bakingbuddy.models.bakes.UpdateBakeInstructionPayload
import com.bakingbuddy.models.bakes.UpdateBakePayload
import com.bakingbuddy.models.ingredients.IngredientDeltaEntry
import com.bakingbuddy.models.instructions.InstructionDeltaEntry
import com.bakingbuddy.repositories.helpers.assertNoOpenBake
import com.bakingbuddy.repositories.helpers.completeBakeIngredient
import com.bakingbuddy.repositories.helpers.completeBakeInstruction
import com.bakingbuddy.repositories.helpers.getBakeIngredientPayloadForRow
import com.bakingbuddy.repositories.helpers.getBakeInstructionPayloadForRow
import com.bakingbuddy.repositories.helpers.getBakeRatings
import com.bakingbuddy.repositories.helpers.getBestIngredientDeltas
import com.bakingbuddy.repositories.helpers.getBestInstructionDeltas
import com.bakingbuddy.repositories.helpers.upsertBakeRatings
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import kotlin.uuid.Uuid

class BakeRepositoryImpl : BakeRepository {
  override suspend fun createBake(recipeId: Uuid): Bake =
    transaction {
      RecipesTable
        .selectAll()
        .where { RecipesTable.id eq recipeId }
        .singleOrNull() ?: throw NotFoundException("Recipe", recipeId.toString())

      assertNoOpenBake(recipeId)

      // Pull the current best_version delta for every ingredient/instruction of this recipe.
      val ingredientDeltas = getBestIngredientDeltas(recipeId)
      val instructionDeltas = getBestInstructionDeltas(recipeId)

      val bakeId = Uuid.random()
      val createdAt = Instant.now()

      BakesTable.insert {
        it[BakesTable.id] = bakeId
        it[BakesTable.recipe_id] = recipeId
        it[BakesTable.created_at] = createdAt
        it[BakesTable.start_datetime] = createdAt
      }

      ingredientDeltas.forEach { delta ->
        BakeIngredientsTable.insert {
          it[BakeIngredientsTable.id] = delta.bakeIngredientId
          it[BakeIngredientsTable.bake_id] = bakeId
          it[BakeIngredientsTable.ingredient_delta_id] = delta.bestDelta.id
          it[BakeIngredientsTable.amount] = delta.bestDelta.amount
          it[BakeIngredientsTable.name] = delta.bestDelta.name
          it[BakeIngredientsTable.notes] = delta.bestDelta.notes
          it[BakeIngredientsTable.order] = delta.bestDelta.order
        }
      }

      instructionDeltas.forEach { delta ->
        BakeInstructionsTable.insert {
          it[BakeInstructionsTable.id] = delta.bakeInstructionId
          it[BakeInstructionsTable.bake_id] = bakeId
          it[BakeInstructionsTable.instruction_delta_id] = delta.bestDelta.id
          it[BakeInstructionsTable.description] = delta.bestDelta.description
          it[BakeInstructionsTable.notes] = delta.bestDelta.notes
          it[BakeInstructionsTable.order] = delta.bestDelta.order
        }
      }

      val bakeDetail =
        BakeDetail(
          id = bakeId,
          recipeId = recipeId,
          createdAt = createdAt,
          startDatetime = createdAt,
        )

      Bake(
        id = bakeId,
        recipeId = recipeId,
        details = bakeDetail,
        ingredientVersions =
          ingredientDeltas.map { delta ->
            BakeIngredientPayload(
              bakeIngredientId = delta.bakeIngredientId,
              initialDeltaValues =
                IngredientDeltaEntry(
                  id = delta.bestDelta.id,
                  ingredientId = delta.bestDelta.ingredientId,
                  version = delta.bestDelta.version,
                  amount = delta.bestDelta.amount,
                  name = delta.bestDelta.name,
                  notes = delta.bestDelta.notes,
                  order = delta.bestDelta.order,
                  createdAt = delta.bestDelta.createdAt,
                ),
              updatedDeltaValues =
                BakeIngredient(
                  updatedAmount = delta.bestDelta.amount,
                  updatedName = delta.bestDelta.name,
                  updatedNotes = delta.bestDelta.notes,
                  updatedOrder = delta.bestDelta.order,
                ),
              completedBakeDeltaId = null,
            )
          },
        instructionVersions =
          instructionDeltas.map { delta ->
            BakeInstructionPayload(
              bakeInstructionId = delta.bakeInstructionId,
              initialDeltaValues =
                InstructionDeltaEntry(
                  id = delta.bestDelta.id,
                  instructionId = delta.bestDelta.instructionId,
                  version = delta.bestDelta.version,
                  description = delta.bestDelta.description,
                  notes = delta.bestDelta.notes,
                  order = delta.bestDelta.order,
                  createdAt = delta.bestDelta.createdAt,
                ),
              updatedDeltaValues =
                BakeInstruction(
                  updatedDescription = delta.bestDelta.description,
                  updatedNotes = delta.bestDelta.notes,
                  updatedOrder = delta.bestDelta.order,
                ),
              completedBakeDeltaId = null,
            )
          },
      )
    }

  // Load all bakes with ingredients and instructions
  override suspend fun listBakesWithProcedure(recipeId: Uuid): List<Bake> {
    return transaction {
      RecipesTable
        .selectAll()
        .where { RecipesTable.id eq recipeId }
        .singleOrNull() ?: throw NotFoundException("Recipe", recipeId.toString())

      val bakeRows =
        BakesTable
          .selectAll()
          .where { BakesTable.recipe_id eq recipeId }
          .toList()

      if (bakeRows.isEmpty()) return@transaction emptyList()
      val bakeIds = bakeRows.map { it[BakesTable.id] }
      val ratingsByBakeId = getBakeRatings(bakeIds)

      val ingredientVersionsByBake =
        BakeIngredientsTable
          .join(
            IngredientDeltaTable,
            JoinType.INNER,
            onColumn = BakeIngredientsTable.ingredient_delta_id,
            otherColumn = IngredientDeltaTable.id,
          ).selectAll()
          .where { BakeIngredientsTable.bake_id inList bakeIds }
          .orderBy(BakeIngredientsTable.order to SortOrder.ASC)
          .map { row ->
            row[BakeIngredientsTable.bake_id] to
              getBakeIngredientPayloadForRow(row)
          }.groupBy({ it.first }, { it.second })

      val instructionVersionsByBake =
        BakeInstructionsTable
          .join(
            InstructionDeltaTable,
            JoinType.INNER,
            onColumn = BakeInstructionsTable.instruction_delta_id,
            otherColumn = InstructionDeltaTable.id,
          ).selectAll()
          .where { BakeInstructionsTable.bake_id inList bakeIds }
          .orderBy(BakeInstructionsTable.order to SortOrder.ASC)
          .map { row ->
            row[BakeInstructionsTable.bake_id] to
              getBakeInstructionPayloadForRow(row)
          }.groupBy({ it.first }, { it.second })

      bakeRows.map { row ->
        val bakeId = row[BakesTable.id]
        val bakeDetail =
          BakeDetail(
            id = bakeId,
            recipeId = row[BakesTable.recipe_id],
            elevation = row[BakesTable.elevation],
            notes = row[BakesTable.notes],
            createdAt = row[BakesTable.created_at],
            startDatetime = row[BakesTable.start_datetime],
            endDatetime = row[BakesTable.end_datetime],
            ratings = ratingsByBakeId[row[BakesTable.id]],
          )
        Bake(
          id = bakeId,
          recipeId = row[BakesTable.recipe_id],
          details = bakeDetail,
          ingredientVersions = ingredientVersionsByBake[bakeId] ?: emptyList(),
          instructionVersions = instructionVersionsByBake[bakeId] ?: emptyList(),
        )
      }
    }
  }

  // Load all details of all bakes
  override suspend fun listBakes(): List<BakeDetail> =
    transaction {
      val bakeRows =
        BakesTable
          .selectAll()
          .orderBy(
            BakesTable.end_datetime to SortOrder.DESC_NULLS_FIRST,
            BakesTable.start_datetime to SortOrder.DESC,
          ).toList()

      val bakeIds = bakeRows.map { it[BakesTable.id] }
      val ratingsByBakeId = getBakeRatings(bakeIds)

      bakeRows.map { row ->
        BakeDetail(
          id = row[BakesTable.id],
          recipeId = row[BakesTable.recipe_id],
          elevation = row[BakesTable.elevation],
          notes = row[BakesTable.notes],
          createdAt = row[BakesTable.created_at],
          startDatetime = row[BakesTable.start_datetime],
          endDatetime = row[BakesTable.end_datetime],
          ratings = ratingsByBakeId[row[BakesTable.id]],
        )
      }
    }

  // Load all details of all bakes for a certain recipe
  override suspend fun listBakesForRecipe(recipeId: Uuid): List<BakeDetail> {
    return transaction {
      RecipesTable
        .selectAll()
        .where { RecipesTable.id eq recipeId }
        .singleOrNull() ?: throw NotFoundException("Recipe", recipeId.toString())

      val bakeRows =
        BakesTable
          .selectAll()
          .where { BakesTable.recipe_id eq recipeId }
          .toList()

      if (bakeRows.isEmpty()) return@transaction emptyList()

      val bakeIds = bakeRows.map { it[BakesTable.id] }
      val ratingsByBakeId = getBakeRatings(bakeIds)

      bakeRows.map { row ->
        val bakeId = row[BakesTable.id]
        BakeDetail(
          id = bakeId,
          recipeId = row[BakesTable.recipe_id],
          elevation = row[BakesTable.elevation],
          notes = row[BakesTable.notes],
          createdAt = row[BakesTable.created_at],
          startDatetime = row[BakesTable.start_datetime],
          endDatetime = row[BakesTable.end_datetime],
          ratings = ratingsByBakeId[bakeId],
        )
      }
    }
  }

  // Load a single bake with ingredients and instructions
  override suspend fun getBake(bakeId: Uuid): Bake =
    transaction {
      val row =
        BakesTable
          .selectAll()
          .where { BakesTable.id eq bakeId }
          .singleOrNull()
          ?: throw NotFoundException("Bake", bakeId.toString())

      val ingredientVersions =
        BakeIngredientsTable
          .join(
            IngredientDeltaTable,
            JoinType.INNER,
            onColumn = BakeIngredientsTable.ingredient_delta_id,
            otherColumn = IngredientDeltaTable.id,
          ).selectAll()
          .where { BakeIngredientsTable.bake_id eq bakeId }
          .orderBy(BakeIngredientsTable.order to SortOrder.ASC)
          .map { row -> getBakeIngredientPayloadForRow(row) }

      val instructionVersions =
        BakeInstructionsTable
          .join(
            InstructionDeltaTable,
            JoinType.INNER,
            onColumn = BakeInstructionsTable.instruction_delta_id,
            otherColumn = InstructionDeltaTable.id,
          ).selectAll()
          .where { BakeInstructionsTable.bake_id eq bakeId }
          .orderBy(BakeInstructionsTable.order to SortOrder.ASC)
          .map { row -> getBakeInstructionPayloadForRow(row) }

      val ratings = getBakeRatings(listOf(bakeId))[bakeId]

      val bakeDetail =
        BakeDetail(
          id = bakeId,
          recipeId = row[BakesTable.recipe_id],
          elevation = row[BakesTable.elevation],
          notes = row[BakesTable.notes],
          createdAt = row[BakesTable.created_at],
          startDatetime = row[BakesTable.start_datetime],
          endDatetime = row[BakesTable.end_datetime],
          ratings = ratings,
        )

      Bake(
        id = bakeId,
        recipeId = row[BakesTable.recipe_id],
        details = bakeDetail,
        ingredientVersions = ingredientVersions,
        instructionVersions = instructionVersions,
      )
    }

  override suspend fun updateBake(payload: UpdateBakePayload): Unit =
    transaction {
      BakesTable
        .selectAll()
        .where { BakesTable.id eq payload.bakeId }
        .singleOrNull() ?: throw NotFoundException("Bake", payload.bakeId.toString())

      if (payload.elevation is PatchField.Present || payload.notes is PatchField.Present) {
        BakesTable.update({ BakesTable.id eq payload.bakeId }) { statement ->
          when (val elevation = payload.elevation) {
            is PatchField.Absent -> {}
            is PatchField.Present -> statement[BakesTable.elevation] = elevation.value
          }
          when (val notes = payload.notes) {
            is PatchField.Absent -> {}
            is PatchField.Present -> statement[BakesTable.notes] = notes.value
          }
        }
      }

      upsertBakeRatings(payload.bakeId, payload.ratings)
    }

  override suspend fun deleteBake(id: Uuid): Unit =
    transaction {
      val existing =
        BakesTable
          .selectAll()
          .where { BakesTable.id eq id }
          .singleOrNull() ?: throw NotFoundException("Bake", id.toString())

      BakeIngredientsTable.deleteWhere { BakeIngredientsTable.bake_id eq id }
      BakeInstructionsTable.deleteWhere { BakeInstructionsTable.bake_id eq id }
      BakesTable.deleteWhere { BakesTable.id eq id }
    }

  override suspend fun updateBakeInstruction(
    bakeId: Uuid,
    payload: UpdateBakeInstructionPayload,
  ): Unit =
    transaction {
      BakeInstructionsTable
        .selectAll()
        .where { (BakeInstructionsTable.id eq payload.bakeInstructionId) and (BakeInstructionsTable.bake_id eq bakeId) }
        .singleOrNull()
        ?: throw NotFoundException("Bake instruction", "bakeId=$bakeId, bakeInstructionId=${payload.bakeInstructionId}")

      BakeInstructionsTable.update({ BakeInstructionsTable.id eq payload.bakeInstructionId }) { statement ->
        statement[BakeInstructionsTable.description] = payload.description

        when (val notes = payload.notes) {
          is PatchField.Absent -> {}
          is PatchField.Present -> statement[BakeInstructionsTable.notes] = notes.value
        }
      }
    }

  override suspend fun updateBakeIngredient(
    bakeId: Uuid,
    payload: UpdateBakeIngredientPayload,
  ): Unit =
    transaction {
      BakeIngredientsTable
        .selectAll()
        .where { (BakeIngredientsTable.id eq payload.bakeIngredientId) and (BakeIngredientsTable.bake_id eq bakeId) }
        .singleOrNull()
        ?: throw NotFoundException("Bake ingredient", "bakeId=$bakeId, bakeIngredientId=${payload.bakeIngredientId}")

      BakeIngredientsTable.update({ BakeIngredientsTable.id eq payload.bakeIngredientId }) { statement ->
        statement[BakeIngredientsTable.amount] = payload.amount
        statement[BakeIngredientsTable.name] = payload.name

        when (val notes = payload.notes) {
          is PatchField.Absent -> {}
          is PatchField.Present -> statement[BakeIngredientsTable.notes] = notes.value
        }
      }
    }

  override suspend fun completeBake(
    bakeId: Uuid,
    payload: CompleteBakePayload,
  ): Unit =
    transaction {
      val now = Instant.now()

      val bakeRow =
        BakesTable
          .selectAll()
          .where { BakesTable.id eq bakeId }
          .singleOrNull() ?: throw NotFoundException("Bake", bakeId.toString())

      if (bakeRow[BakesTable.end_datetime] != null) {
        throw ConflictException("bakeAlreadyComplete")
      }

      BakesTable.update({ BakesTable.id eq bakeId }) {
        it[BakesTable.end_datetime] = now
      }

      BakeIngredientsTable
        .selectAll()
        .where { BakeIngredientsTable.bake_id eq bakeId }
        .toList()
        .forEach { row -> completeBakeIngredient(row, payload.setDeltasAsBest, now) }

      BakeInstructionsTable
        .selectAll()
        .where { BakeInstructionsTable.bake_id eq bakeId }
        .toList()
        .forEach { row -> completeBakeInstruction(row, payload.setDeltasAsBest, now) }
    }
}
