package com.bakingbuddy.repositories

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
import com.bakingbuddy.models.ingredients.CreateIngredientPayload
import com.bakingbuddy.models.ingredients.EditIngredientPayload
import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.instructions.EditInstructionPayload
import com.bakingbuddy.models.instructions.Instruction
import com.bakingbuddy.models.recipes.CreateRecipePayload
import com.bakingbuddy.models.recipes.EditRecipePayload
import com.bakingbuddy.models.recipes.Recipe
import com.bakingbuddy.models.recipes.RecipeDetail
import org.jetbrains.exposed.v1.core.JoinType
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

class RecipeRepositoryImpl : RecipeRepository {
    override suspend fun findById(id: Uuid): Recipe? {
        return transaction {
            val recipeRow =
                RecipesTable
                    .selectAll()
                    .where { RecipesTable.id eq id }
                    .singleOrNull() ?: return@transaction null

            val ingredients = getIngredientsForRecipe(id)
            val instructions = getInstructionsForRecipe(id)

            val details =
                RecipeDetail(
                    id = recipeRow[RecipesTable.id],
                    name = recipeRow[RecipesTable.name],
                    description = recipeRow[RecipesTable.description],
                    createdAt = recipeRow[RecipesTable.created_at],
                    recipeSource = recipeRow[RecipesTable.recipe_source],
                    tags = recipeRow[RecipesTable.tags],
                    tools = recipeRow[RecipesTable.tools],
                    notes = recipeRow[RecipesTable.notes],
                )

            Recipe(
                id = recipeRow[RecipesTable.id],
                details = details,
                ingredients = ingredients,
                instructions = instructions,
            )
        }
    }

    fun getIngredientsForRecipe(recipeId: Uuid): List<Ingredient> {
        val ingredients =
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
                    .map { row ->
                        Ingredient(
                            id = row[IngredientsTable.id],
                            recipeId = row[IngredientsTable.recipe_id],
                            bestVersion = row[IngredientsTable.best_version],
                            notes = row[IngredientsTable.notes],
                            createdAt = row[IngredientsTable.created_at],
                            amount = row[IngredientDeltaTable.amount],
                            name = row[IngredientDeltaTable.name],
                        )
                    }
            }
        val ingredientConceptCount =
            IngredientsTable
                .selectAll()
                .where { IngredientsTable.recipe_id eq recipeId }
                .count()

        if (ingredients.size.toLong() != ingredientConceptCount) {
            throw DataIntegrityException(
                "Missing ingredient_delta row for best_version on one or more ingredients of recipe $recipeId",
            )
        }

        return ingredients
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
                        notes = row[InstructionsTable.notes],
                        createdAt = row[InstructionsTable.created_at],
                        description = row[InstructionDeltaTable.description],
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

    override suspend fun listAll(): List<RecipeDetail> =
        transaction {
            RecipesTable
                .selectAll()
                .map { row ->
                    RecipeDetail(
                        id = row[RecipesTable.id],
                        name = row[RecipesTable.name],
                        description = row[RecipesTable.description],
                        recipeSource = row[RecipesTable.recipe_source],
                        tags = row[RecipesTable.tags],
                        createdAt = row[RecipesTable.created_at],
                        tools = row[RecipesTable.tools],
                        notes = row[RecipesTable.notes],
                    )
                }
        }

    override suspend fun create(request: CreateRecipePayload): Recipe {
        val recipeId = Uuid.random()
        val createdAt = Instant.now()

        return transaction {
            val recipeStatement =
                RecipesTable.insert {
                    it[RecipesTable.id] = recipeId
                    it[RecipesTable.name] = request.name
                    it[RecipesTable.description] = request.description
                    it[RecipesTable.recipe_source] = request.recipeSource.orEmpty()
                    it[RecipesTable.tags] = request.tags.orEmpty()
                    it[RecipesTable.tools] = request.tools.orEmpty()
                    it[RecipesTable.created_at] = createdAt
                }

            val ingredients = createIngredients(recipeId, request.ingredients)
            val instructions = createInstructions(recipeId, request.instructions)

            val details =
                RecipeDetail(
                    id = recipeId,
                    name = request.name,
                    description = request.description,
                    recipeSource = request.recipeSource,
                    tags = request.tags,
                    tools = request.tools,
                    createdAt = createdAt,
                    notes = null,
                )

            Recipe(
                id = recipeId,
                details = details,
                ingredients = ingredients,
                instructions = instructions,
            )
        }
    }

    fun createIngredients(
        recipeId: Uuid,
        request: List<CreateIngredientPayload>,
    ): List<Ingredient> =
        request.map { ingredient ->
            val ingredientId = Uuid.random()
            val createdAt = Instant.now()

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
            }

            Ingredient(
                id = ingredientId,
                recipeId = recipeId,
                bestVersion = 1,
                amount = ingredient.amount,
                name = ingredient.name,
                notes = null,
                createdAt = ingredientStatement[IngredientsTable.created_at],
            )
        }

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
            )
        }

    override suspend fun editRecipe(
        id: Uuid,
        request: EditRecipePayload,
    ): Recipe =
        transaction {
            val existing =
                RecipesTable
                    .selectAll()
                    .where { RecipesTable.id eq id }
                    .singleOrNull() ?: throw NotFoundException("Recipe", id.toString())

            // Only touches Recipes table columns — ingredients/instructions are untouched here
            RecipesTable.update({ RecipesTable.id eq id }) {
                request.name?.let { name -> it[RecipesTable.name] = name }
                request.description?.let { description -> it[RecipesTable.description] = description }
                request.recipeSource?.let { source -> it[RecipesTable.recipe_source] = source }
                request.tags?.let { tags -> it[RecipesTable.tags] = tags }
                request.tools?.let { tools -> it[RecipesTable.tools] = tools }
            }

            val updatedRow =
                RecipesTable
                    .selectAll()
                    .where { RecipesTable.id eq id }
                    .single()

            val details =
                RecipeDetail(
                    id = updatedRow[RecipesTable.id],
                    name = updatedRow[RecipesTable.name],
                    description = updatedRow[RecipesTable.description],
                    createdAt = updatedRow[RecipesTable.created_at],
                    recipeSource = updatedRow[RecipesTable.recipe_source],
                    tags = updatedRow[RecipesTable.tags],
                    tools = updatedRow[RecipesTable.tools],
                    notes = updatedRow[RecipesTable.notes],
                )

            Recipe(
                id = updatedRow[RecipesTable.id],
                details = details,
                ingredients = getIngredientsForRecipe(id),
                instructions = getInstructionsForRecipe(id),
            )
        }

    override suspend fun editIngredient(
        ingredientId: Uuid,
        request: EditIngredientPayload,
    ): Ingredient =
        transaction {
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
            val createdAt = Instant.now()

            IngredientDeltaTable.insert {
                it[IngredientDeltaTable.ingredient_id] = ingredientId
                it[IngredientDeltaTable.version] = newVersion
                it[IngredientDeltaTable.amount] = request.amount
                it[IngredientDeltaTable.name] = request.name
                it[IngredientDeltaTable.created_at] = createdAt
            }

            if (request.setAsBestVersion ?: false) {
                IngredientsTable.update({ IngredientsTable.id eq ingredientId }) {
                    it[IngredientsTable.best_version] = newVersion
                }
            }

            Ingredient(
                id = ingredientId,
                recipeId = ingredientRow[IngredientsTable.recipe_id],
                bestVersion = if (request.setAsBestVersion ?: false) newVersion else ingredientRow[IngredientsTable.best_version],
                notes = ingredientRow[IngredientsTable.notes],
                createdAt = ingredientRow[IngredientsTable.created_at],
                amount = request.amount,
                name = request.name,
            )
        }

    override suspend fun editInstruction(
        instructionId: Uuid,
        request: EditInstructionPayload,
    ): Instruction =
        transaction {
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
            val createdAt = Instant.now()

            InstructionDeltaTable.insert {
                it[InstructionDeltaTable.instruction_id] = instructionId
                it[InstructionDeltaTable.version] = newVersion
                it[InstructionDeltaTable.description] = request.description
                it[InstructionDeltaTable.created_at] = createdAt
            }

            if (request.setAsBestVersion ?: false) {
                InstructionsTable.update({ InstructionsTable.id eq instructionId }) {
                    it[InstructionsTable.best_version] = newVersion
                }
            }

            Instruction(
                id = instructionId,
                recipeId = instructionRow[InstructionsTable.recipe_id],
                bestVersion = if (request.setAsBestVersion ?: false) newVersion else instructionRow[InstructionsTable.best_version],
                notes = instructionRow[InstructionsTable.notes],
                createdAt = instructionRow[InstructionsTable.created_at],
                description = request.description,
            )
        }

    override suspend fun updateRecipeNotes(
        recipeId: Uuid,
        notes: String?,
    ) = transaction {
        RecipesTable
            .selectAll()
            .where { RecipesTable.id eq recipeId }
            .singleOrNull() ?: throw NotFoundException("Recipe", recipeId.toString())

        val updatedRows =
            RecipesTable.update({ RecipesTable.id eq recipeId }) {
                it[RecipesTable.notes] = notes
            }
    }

    override suspend fun updateIngredientNotes(
        ingredientId: Uuid,
        notes: String?,
    ) = transaction {
        IngredientsTable
            .selectAll()
            .where { IngredientsTable.id eq ingredientId }
            .singleOrNull() ?: throw NotFoundException("Ingredient", ingredientId.toString())

        val updatedRows =
            IngredientsTable.update({ IngredientsTable.id eq ingredientId }) {
                it[IngredientsTable.notes] = notes
            }
    }

    override suspend fun updateInstructionNotes(
        instructionId: Uuid,
        notes: String?,
    ) = transaction {
        InstructionsTable
            .selectAll()
            .where { InstructionsTable.id eq instructionId }
            .singleOrNull() ?: throw NotFoundException("Instruction", instructionId.toString())

        val updatedRows =
            InstructionsTable.update({ InstructionsTable.id eq instructionId }) {
                it[InstructionsTable.notes] = notes
            }
    }

    override suspend fun deleteRecipe(id: Uuid): Unit =
        transaction {
            val existing =
                RecipesTable
                    .selectAll()
                    .where { RecipesTable.id eq id }
                    .singleOrNull() ?: throw NotFoundException("Recipe", id.toString())

            val ingredientIds =
                IngredientsTable
                    .selectAll()
                    .where { IngredientsTable.recipe_id eq id }
                    .map { it[IngredientsTable.id] }

            val instructionIds =
                InstructionsTable
                    .selectAll()
                    .where { InstructionsTable.recipe_id eq id }
                    .map { it[InstructionsTable.id] }

            val bakeIds =
                BakesTable
                    .selectAll()
                    .where { BakesTable.recipe_id eq id }
                    .map { it[BakesTable.id] }

            if (bakeIds.isNotEmpty()) {
                BakeIngredientsTable.deleteWhere { BakeIngredientsTable.bake_id inList bakeIds }
                BakeInstructionsTable.deleteWhere { BakeInstructionsTable.bake_id inList bakeIds }
            }
            BakesTable.deleteWhere { BakesTable.recipe_id eq id }

            if (ingredientIds.isNotEmpty()) {
                IngredientDeltaTable.deleteWhere { IngredientDeltaTable.ingredient_id inList ingredientIds }
            }
            IngredientsTable.deleteWhere { IngredientsTable.recipe_id eq id }

            if (instructionIds.isNotEmpty()) {
                InstructionDeltaTable.deleteWhere { InstructionDeltaTable.instruction_id inList instructionIds }
            }
            InstructionsTable.deleteWhere { InstructionsTable.recipe_id eq id }

            RecipesTable.deleteWhere { RecipesTable.id eq id }
        }
}
