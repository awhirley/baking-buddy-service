package com.bakingbuddy.repositories

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
import com.bakingbuddy.database.RecipesTable
import com.bakingbuddy.models.bakes.Bake
import com.bakingbuddy.models.bakes.BakeDetail
import com.bakingbuddy.models.bakes.BakeIngredientPayload
import com.bakingbuddy.models.bakes.BakeInstructionPayload
import com.bakingbuddy.models.bakes.BakeRating
import com.bakingbuddy.models.bakes.CompleteBakePayload
import com.bakingbuddy.models.bakes.UpdateBakeIngredientPayload
import com.bakingbuddy.models.bakes.UpdateBakeInstructionPayload
import com.bakingbuddy.models.bakes.UpdateBakePayload
import com.bakingbuddy.models.bakes.UpdateBakeRatingPayload
import com.bakingbuddy.repositories.helpers.BestIngredientDelta
import com.bakingbuddy.repositories.helpers.BestInstructionDelta
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import kotlin.uuid.Uuid

@Suppress("TooManyFunctions", "LargeClass")
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
            notes = row[IngredientsTable.notes],
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
            notes = row[InstructionsTable.notes],
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
      val recipe =
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

      val bakeIngredientId = Uuid.random()
      ingredientDeltas.forEach { delta ->
        BakeIngredientsTable.insert {
          it[BakeIngredientsTable.id] = bakeIngredientId
          it[BakeIngredientsTable.bake_id] = bakeId
          it[BakeIngredientsTable.ingredient_delta_id] = delta.deltaId
          it[BakeIngredientsTable.amount] = null
          it[BakeIngredientsTable.name] = null
          it[BakeIngredientsTable.notes] = null
        }
      }

      val bakeInstructionId = Uuid.random()
      instructionDeltas.forEach { delta ->
        BakeInstructionsTable.insert {
          it[BakeInstructionsTable.id] = bakeInstructionId
          it[BakeInstructionsTable.bake_id] = bakeId
          it[BakeInstructionsTable.instruction_delta_id] = delta.deltaId
          it[BakeInstructionsTable.description] = null
          it[BakeInstructionsTable.notes] = null
        }
      }

      val bakeDetail =
        BakeDetail(
          id = bakeId,
          recipeName = recipe[RecipesTable.name],
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
              bakeIngredientId = bakeIngredientId,
              ingredientId = delta.ingredientId,
              ingredientDeltaId = delta.deltaId,
              version = delta.version,
              amount = delta.amount,
              name = delta.name,
              notes = delta.notes,
            )
          },
        instructionVersions =
          instructionDeltas.map { delta ->
            BakeInstructionPayload(
              bakeInstructionId = bakeInstructionId,
              instructionId = delta.instructionId,
              instructionDeltaId = delta.deltaId,
              version = delta.version,
              description = delta.description,
              notes = delta.notes,
            )
          },
      )
    }

  // Load all bakes with ingredients and instructions
  override suspend fun listBakesWithProcedure(recipeId: Uuid): List<Bake> {
    return transaction {
      val recipe =
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
          .map { row ->
            row[BakeIngredientsTable.bake_id] to
              BakeIngredientPayload(
                bakeIngredientId = row[BakeIngredientsTable.id],
                ingredientId = row[IngredientDeltaTable.ingredient_id],
                ingredientDeltaId = row[BakeIngredientsTable.ingredient_delta_id],
                version = row[IngredientDeltaTable.version],
                amount = row[IngredientDeltaTable.amount],
                name = row[IngredientDeltaTable.name],
                notes = row[IngredientDeltaTable.notes],
                updatedAmount = row[BakeIngredientsTable.amount],
                updatedName = row[BakeIngredientsTable.name],
                updatedNotes = row[BakeIngredientsTable.notes],
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
            row[BakeInstructionsTable.bake_id] to
              BakeInstructionPayload(
                bakeInstructionId = row[BakeInstructionsTable.id],
                instructionId = row[InstructionDeltaTable.instruction_id],
                instructionDeltaId = row[BakeInstructionsTable.instruction_delta_id],
                version = row[InstructionDeltaTable.version],
                description = row[InstructionDeltaTable.description],
                notes = row[InstructionDeltaTable.notes],
                updatedDescription = row[BakeInstructionsTable.description],
                updatedNotes = row[BakeInstructionsTable.notes],
              )
          }.groupBy({ it.first }, { it.second })

      bakeRows.map { row ->
        val bakeId = row[BakesTable.id]
        val bakeDetail =
          BakeDetail(
            id = bakeId,
            recipeId = row[BakesTable.recipe_id],
            recipeName = recipe[RecipesTable.name],
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

  // Load all details of all bakes
  override suspend fun listBakes(): List<BakeDetail> =
    transaction {
      val bakeRows =
        BakesTable
          .join(RecipesTable, JoinType.INNER, onColumn = BakesTable.recipe_id, otherColumn = RecipesTable.id)
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
          recipeName = row[RecipesTable.name],
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
      val recipe =
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
          recipeName = recipe[RecipesTable.name],
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
          .join(RecipesTable, JoinType.INNER, onColumn = BakesTable.recipe_id, otherColumn = RecipesTable.id)
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
          .map { row ->
            val bakeIngredientAmount = row[BakeIngredientsTable.amount]
            val bakeIngredientName = row[BakeIngredientsTable.name]
            BakeIngredientPayload(
              bakeIngredientId = row[BakeIngredientsTable.id],
              ingredientId = row[IngredientDeltaTable.ingredient_id],
              ingredientDeltaId = row[BakeIngredientsTable.ingredient_delta_id],
              version = row[IngredientDeltaTable.version],
              amount = row[IngredientDeltaTable.amount],
              name = row[IngredientDeltaTable.name],
              notes = row[IngredientDeltaTable.notes],
              updatedAmount = row[BakeIngredientsTable.amount],
              updatedName = row[BakeIngredientsTable.name],
              updatedNotes = row[BakeIngredientsTable.notes],
            )
          }

      val instructionVersions =
        BakeInstructionsTable
          .join(
            InstructionDeltaTable,
            JoinType.INNER,
            onColumn = BakeInstructionsTable.instruction_delta_id,
            otherColumn = InstructionDeltaTable.id,
          ).selectAll()
          .where { BakeInstructionsTable.bake_id eq bakeId }
          .map { row ->
            BakeInstructionPayload(
              bakeInstructionId = row[BakeInstructionsTable.id],
              instructionId = row[InstructionDeltaTable.instruction_id],
              instructionDeltaId = row[BakeInstructionsTable.instruction_delta_id],
              version = row[InstructionDeltaTable.version],
              description = row[InstructionDeltaTable.description],
              notes = row[InstructionDeltaTable.notes],
              updatedDescription = row[BakeInstructionsTable.description],
              updatedNotes = row[BakeInstructionsTable.notes],
            )
          }

      val ratings = getBakeRatings(listOf(bakeId))[bakeId]

      val bakeDetail =
        BakeDetail(
          id = bakeId,
          recipeId = row[BakesTable.recipe_id],
          recipeName = row[RecipesTable.name],
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

      upsertBakeRatings(payload.bakeId, payload.ratings)
    }

  @Suppress("CyclomaticComplexMethod")
  private fun upsertBakeRatings(
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
      throw ConflictException("bakeAlreadyComplete")
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
      throw DataIntegrityException(
        "BakeIngredient requires both amount and name to be set. Amount: $amount, Name: $name",
      )
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

  private fun assertNoOpenBake(recipeId: Uuid) {
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
}
