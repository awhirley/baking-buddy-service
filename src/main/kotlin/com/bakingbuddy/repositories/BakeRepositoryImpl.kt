package com.bakingbuddy.repositories

import com.bakingbuddy.api.errors.ConflictException
import com.bakingbuddy.api.errors.DataIntegrityException
import com.bakingbuddy.api.errors.NotFoundException
import com.bakingbuddy.database.BakeIngredientsTable
import com.bakingbuddy.database.BakeInstructionsTable
import com.bakingbuddy.database.BakesTable
import com.bakingbuddy.database.IngredientDeltaTable
import com.bakingbuddy.database.IngredientsTable
import com.bakingbuddy.database.InstructionDeltaTable
import com.bakingbuddy.database.InstructionsTable
import com.bakingbuddy.database.RecipesTable
import com.bakingbuddy.models.bakes.Bake
import com.bakingbuddy.models.bakes.BakeDetail
import com.bakingbuddy.models.bakes.BakeIngredientPayload
import com.bakingbuddy.models.bakes.BakeInstructionPayload
import com.bakingbuddy.models.bakes.CompleteBakePayload
import com.bakingbuddy.models.bakes.UpdateBakeIngredientPayload
import com.bakingbuddy.models.bakes.UpdateBakeInstructionPayload
import com.bakingbuddy.models.bakes.UpdateBakePayload
import com.bakingbuddy.repositories.helpers.BestIngredientDelta
import com.bakingbuddy.repositories.helpers.BestInstructionDelta
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import kotlin.uuid.Uuid

@Suppress("TooManyFunctions")
class BakeRepositoryImpl : BakeRepository {
  private fun getBestIngredientDeltas(recipeId: Uuid): List<BestIngredientDelta> {
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
        .map { row ->
          BestIngredientDelta(
            deltaId = row[IngredientDeltaTable.id],
            ingredientId = row[IngredientDeltaTable.ingredient_id],
            version = row[IngredientDeltaTable.version],
            amount = row[IngredientDeltaTable.amount],
            name = row[IngredientDeltaTable.name],
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

  private fun getBestInstructionDeltas(recipeId: Uuid): List<BestInstructionDelta> {
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
        .map { row ->
          BestInstructionDelta(
            deltaId = row[InstructionDeltaTable.id],
            instructionId = row[InstructionDeltaTable.instruction_id],
            version = row[InstructionDeltaTable.version],
            description = row[InstructionDeltaTable.description],
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

  override suspend fun createBake(recipeId: Uuid): Bake =
    transaction {
      RecipesTable
        .selectAll()
        .where { RecipesTable.id eq recipeId }
        .singleOrNull() ?: throw NotFoundException("Recipe", recipeId.toString())

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
          it[BakeIngredientsTable.id] = Uuid.random()
          it[BakeIngredientsTable.bake_id] = bakeId
          it[BakeIngredientsTable.ingredient_delta_id] = delta.deltaId
          it[BakeIngredientsTable.amount] = null
          it[BakeIngredientsTable.name] = null
          it[BakeIngredientsTable.notes] = null
        }
      }

      instructionDeltas.forEach { delta ->
        BakeInstructionsTable.insert {
          it[BakeInstructionsTable.id] = Uuid.random()
          it[BakeInstructionsTable.bake_id] = bakeId
          it[BakeInstructionsTable.instruction_delta_id] = delta.deltaId
          it[BakeInstructionsTable.description] = null
          it[BakeInstructionsTable.notes] = null
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
              ingredientId = delta.ingredientId,
              ingredientDeltaId = delta.deltaId,
              version = delta.version,
              amount = delta.amount,
              name = delta.name,
            )
          },
        instructionVersions =
          instructionDeltas.map { delta ->
            BakeInstructionPayload(
              instructionId = delta.instructionId,
              instructionDeltaId = delta.deltaId,
              version = delta.version,
              description = delta.description,
            )
          },
      )
    }

  // Load all bakes with ingredients and instructions
  override suspend fun listBakesWithProcedure(recipeId: Uuid): List<Bake> {
    return transaction {
      val bakeRows =
        BakesTable
          .selectAll()
          .where { BakesTable.recipe_id eq recipeId }
          .toList()

      if (bakeRows.isEmpty()) return@transaction emptyList()

      val bakeIds = bakeRows.map { it[BakesTable.id] }

      val ingredientVersionsByBake =
        BakeIngredientsTable
          .join(
            IngredientDeltaTable,
            JoinType.INNER,
            onColumn = BakeIngredientsTable.ingredient_delta_id,
            otherColumn = IngredientDeltaTable.id,
          ).selectAll()
          .where { BakeIngredientsTable.bake_id inList bakeIds }
          .map { row ->
            val bakeIngredientAmount = row[BakeIngredientsTable.amount]
            val bakeIngredientName = row[BakeIngredientsTable.name]
            row[BakeIngredientsTable.bake_id] to
              BakeIngredientPayload(
                ingredientId = row[IngredientDeltaTable.ingredient_id],
                ingredientDeltaId =
                  if (bakeIngredientAmount !=
                    null
                  ) {
                    null
                  } else {
                    row[BakeIngredientsTable.ingredient_delta_id]
                  },
                version = if (bakeIngredientAmount != null) null else row[IngredientDeltaTable.version],
                amount = bakeIngredientAmount ?: row[IngredientDeltaTable.amount],
                name = bakeIngredientName ?: row[IngredientDeltaTable.name],
              )
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
          .map { row ->
            val bakeInstructionDescription = row[BakeInstructionsTable.description]
            row[BakeInstructionsTable.bake_id] to
              BakeInstructionPayload(
                instructionId = row[InstructionDeltaTable.instruction_id],
                instructionDeltaId =
                  if (bakeInstructionDescription !=
                    null
                  ) {
                    null
                  } else {
                    row[BakeInstructionsTable.instruction_delta_id]
                  },
                version = if (bakeInstructionDescription != null) null else row[InstructionDeltaTable.version],
                description = bakeInstructionDescription ?: row[InstructionDeltaTable.description],
              )
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
  override suspend fun listBakes(recipeId: Uuid): List<BakeDetail> {
    return transaction {
      val bakeRows =
        BakesTable
          .selectAll()
          .where { BakesTable.recipe_id eq recipeId }
          .toList()

      if (bakeRows.isEmpty()) return@transaction emptyList()

      val bakeIds = bakeRows.map { it[BakesTable.id] }

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
        )
      }
    }
  }

  override suspend fun updateBake(payload: UpdateBakePayload): Unit =
    transaction {
      BakesTable
        .selectAll()
        .where { BakesTable.id eq payload.bakeId }
        .singleOrNull() ?: throw NotFoundException("Bake", payload.bakeId.toString())

      BakesTable.update({ BakesTable.id eq payload.bakeId }) {
        payload.startDatetime?.let { start -> it[BakesTable.start_datetime] = start }
        payload.endDatetime?.let { end -> it[BakesTable.end_datetime] = end }
        payload.elevation?.let { elevation -> it[BakesTable.elevation] = elevation }
        payload.notes?.let { notes -> it[BakesTable.notes] = notes }
      }
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
        .where {
          (BakeInstructionsTable.bake_id eq bakeId) and
            (BakeInstructionsTable.instruction_delta_id eq payload.deltaId)
        }.singleOrNull()
        ?: throw NotFoundException(
          "Bake instruction",
          "bakeId=$bakeId, instructionDeltaId=${payload.deltaId}",
        )

      BakeInstructionsTable.update({
        (BakeInstructionsTable.bake_id eq bakeId) and
          (BakeInstructionsTable.instruction_delta_id eq payload.deltaId)
      }) {
        it[BakeInstructionsTable.description] = payload.description
      }
    }

  override suspend fun updateBakeIngredient(
    bakeId: Uuid,
    payload: UpdateBakeIngredientPayload,
  ): Unit =
    transaction {
      BakeIngredientsTable
        .selectAll()
        .where {
          (BakeIngredientsTable.bake_id eq bakeId) and
            (BakeIngredientsTable.ingredient_delta_id eq payload.deltaId)
        }.singleOrNull()
        ?: throw NotFoundException(
          "Bake ingredient",
          "bakeId=$bakeId, ingredientDeltaId=${payload.deltaId}",
        )

      BakeIngredientsTable.update({
        (BakeIngredientsTable.bake_id eq bakeId) and
          (BakeIngredientsTable.ingredient_delta_id eq payload.deltaId)
      }) {
        it[BakeIngredientsTable.amount] = payload.amount
        it[BakeIngredientsTable.name] = payload.name
      }
    }

  override suspend fun completeBake(
    bakeId: Uuid,
    payload: CompleteBakePayload,
  ): Unit =
    transaction {
      val now = Instant.now()

      markBakeComplete(bakeId, now)

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

  private fun markBakeComplete(
    bakeId: Uuid,
    now: Instant,
  ) {
    val bakeRow =
      BakesTable
        .selectAll()
        .where { BakesTable.id eq bakeId }
        .singleOrNull() ?: throw NotFoundException("Bake", bakeId.toString())

    if (bakeRow[BakesTable.end_datetime] != null) {
      // TODO
      throw ConflictException("Bake")
    }

    BakesTable.update({ BakesTable.id eq bakeId }) {
      it[BakesTable.end_datetime] = now
    }
  }

  private fun completeBakeIngredient(
    row: ResultRow,
    setAsBest: Boolean,
    now: Instant,
  ) {
    val amount = row[BakeIngredientsTable.amount]
    val name = row[BakeIngredientsTable.name]
    val notes = row[BakeIngredientsTable.notes]

    if (amount == null && name == null) return

    if (amount == null || name == null) {
      // TODO
      throw DataIntegrityException("BakeIngredient")
    }

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
    }

    BakeIngredientsTable.update({ BakeIngredientsTable.id eq row[BakeIngredientsTable.id] }) {
      it[BakeIngredientsTable.ingredient_delta_id] = newDeltaId
    }

    if (setAsBest) {
      IngredientsTable.update({ IngredientsTable.id eq ingredientId }) {
        it[IngredientsTable.best_version] = newVersion
      }
    }
  }

  private fun completeBakeInstruction(
    row: ResultRow,
    setAsBest: Boolean,
    now: Instant,
  ) {
    val description = row[BakeInstructionsTable.description] ?: return
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
    }

    BakeInstructionsTable.update({ BakeInstructionsTable.id eq row[BakeInstructionsTable.id] }) {
      it[BakeInstructionsTable.instruction_delta_id] = newDeltaId
    }

    if (setAsBest) {
      InstructionsTable.update({ InstructionsTable.id eq instructionId }) {
        it[InstructionsTable.best_version] = newVersion
      }
    }
  }

  private fun nextIngredientDeltaVersion(ingredientId: Uuid): Int {
    val maxVersionExpr = IngredientDeltaTable.version.max()
    val highestVersion =
      IngredientDeltaTable
        .select(maxVersionExpr)
        .where { IngredientDeltaTable.ingredient_id eq ingredientId }
        .single()[maxVersionExpr] ?: 0

    return highestVersion + 1
  }

  private fun nextInstructionDeltaVersion(instructionId: Uuid): Int {
    val maxVersionExpr = InstructionDeltaTable.version.max()
    val highestVersion =
      InstructionDeltaTable
        .select(maxVersionExpr)
        .where { InstructionDeltaTable.instruction_id eq instructionId }
        .single()[maxVersionExpr] ?: 0

    return highestVersion + 1
  }
}
