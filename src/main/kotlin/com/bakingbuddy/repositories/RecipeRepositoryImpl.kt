package com.bakingbuddy.repositories

import com.bakingbuddy.database.BakeIngredients
import com.bakingbuddy.database.BakeInstructions
import com.bakingbuddy.database.Bakes
import com.bakingbuddy.database.IngredientDelta
import com.bakingbuddy.database.Ingredients
import com.bakingbuddy.database.InstructionDelta
import com.bakingbuddy.database.Instructions
import com.bakingbuddy.database.Recipes
import com.bakingbuddy.models.ingredients.CreateIngredientPayload
import com.bakingbuddy.models.recipes.CreateRecipePayload
import com.bakingbuddy.models.ingredients.EditIngredientPayload
import com.bakingbuddy.models.instructions.EditInstructionPayload
import com.bakingbuddy.models.recipes.EditRecipePayload
import com.bakingbuddy.models.ingredients.Ingredient
import com.bakingbuddy.models.instructions.Instruction
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

    override suspend fun findById(id: Uuid): RecipeDetail? {
        return transaction {
            val recipeRow = Recipes
                .selectAll()
                .where { Recipes.id eq id }
                .singleOrNull() ?: return@transaction null

            val ingredients = getIngredientsForRecipe(id)
            val instructions = getInstructionsForRecipe(id)

            RecipeDetail(
                id = recipeRow[Recipes.id],
                name = recipeRow[Recipes.name],
                description = recipeRow[Recipes.description],
                createdAt = recipeRow[Recipes.created_at],
                recipeSource = recipeRow[Recipes.recipe_source],
                tags = recipeRow[Recipes.tags],
                tools = recipeRow[Recipes.tools],
                ingredients = ingredients,
                instructions = instructions,
                notes = recipeRow[Recipes.notes],
            )
        }
    }
    
    fun getIngredientsForRecipe(recipeId: Uuid): List<Ingredient> {
        val ingredients = transaction {
            val ingredientJoin = Ingredients.join(
                IngredientDelta,
                JoinType.INNER,
                onColumn = Ingredients.id,
                otherColumn = IngredientDelta.ingredient_id,
                additionalConstraint = { IngredientDelta.version eq Ingredients.best_version },
            )

            ingredientJoin
                .selectAll()
                .where { Ingredients.recipe_id eq recipeId }
                .map { row ->
                    Ingredient(
                        id = row[Ingredients.id],
                        recipeId = row[Ingredients.recipe_id],
                        bestVersion = row[Ingredients.best_version],
                        notes = row[Ingredients.notes],
                        createdAt = row[Ingredients.created_at],
                        amount = row[IngredientDelta.amount],
                        name = row[IngredientDelta.name],
                    )
                }
        }
        val ingredientConceptCount = Ingredients
            .selectAll()
            .where { Ingredients.recipe_id eq recipeId }
            .count()

        check(ingredients.size.toLong() == ingredientConceptCount) {
            "Missing ingredient_delta row for best_version on one or more ingredients of recipe $recipeId"
        }
        
        return ingredients
    }
    
    fun getInstructionsForRecipe(recipeId: Uuid): List<Instruction> {
        val instructionJoin = Instructions.join(
            InstructionDelta,
            JoinType.INNER,
            onColumn = Instructions.id,
            otherColumn = InstructionDelta.instruction_id,
            additionalConstraint = { InstructionDelta.version eq Instructions.best_version },
        )

        val instructions = instructionJoin
            .selectAll()
            .where { Instructions.recipe_id eq recipeId }
            .map { row ->
                Instruction(
                    id = row[Instructions.id],
                    recipeId = row[Instructions.recipe_id],
                    bestVersion = row[Instructions.best_version],
                    notes = row[Instructions.notes],
                    createdAt = row[Instructions.created_at],
                    description = row[InstructionDelta.description],
                )
            }

        val instructionConceptCount = Instructions
            .selectAll()
            .where { Instructions.recipe_id eq recipeId }
            .count()

        check(instructions.size.toLong() == instructionConceptCount) {
            "Missing instruction_delta row for best_version on one or more instructions of recipe $recipeId"
        }

        return instructions
    }

    override suspend fun listAll(): List<Recipe> {
        return transaction {
            Recipes
                .selectAll()
                .map { row ->
                    Recipe(
                        id = row[Recipes.id],
                        name = row[Recipes.name],
                        description = row[Recipes.description],
                        recipeSource = row[Recipes.recipe_source],
                        tags = row[Recipes.tags],
                        createdAt = row[Recipes.created_at],
                    )
                }
        }
    }
    
    override suspend fun create(request: CreateRecipePayload): RecipeDetail {
        val recipeId = Uuid.random()
        val createdAt = Instant.now()
        
        return transaction {
            val recipeStatement = Recipes.insert {
                it[Recipes.id] = recipeId
                it[Recipes.name] = request.name
                it[Recipes.description] = request.description
                it[Recipes.recipe_source] = request.recipeSource.orEmpty()
                it[Recipes.tags] = request.tags.orEmpty()
                it[Recipes.tools] = request.tools.orEmpty()
                it[Recipes.created_at] = createdAt
            }

            val ingredients = createIngredients(recipeId, request.ingredients)
            val instructions = createInstructions(recipeId, request.instructions)

            RecipeDetail(
                id = recipeId,
                name = request.name,
                description = request.description,
                recipeSource = request.recipeSource,
                tags = request.tags,
                tools = request.tools,
                createdAt = createdAt,
                ingredients = ingredients,
                instructions = instructions,
                notes = null
            )
        }
    }

    fun createIngredients(recipeId: Uuid, request: List<CreateIngredientPayload>): List<Ingredient> {
        return request.map { ingredient ->
            val ingredientId = Uuid.random()
            val createdAt = Instant.now()
            
            val ingredientStatement = Ingredients.insert {
                it[Ingredients.id] = ingredientId
                it[Ingredients.recipe_id] = recipeId
                it[Ingredients.best_version] = 1
                it[Ingredients.created_at] = createdAt
            }

            IngredientDelta.insert {
                it[IngredientDelta.ingredient_id] = ingredientId
                it[IngredientDelta.version] = 1
                it[IngredientDelta.amount] = ingredient.amount
                it[IngredientDelta.name] = ingredient.name
                it[IngredientDelta.created_at] = createdAt
            }

            Ingredient(
                id = ingredientId,
                recipeId = recipeId,
                bestVersion = 1,
                amount = ingredient.amount,
                name = ingredient.name,
                notes = null,
                createdAt = ingredientStatement[Ingredients.created_at],
            )
        }
    }

    fun createInstructions(recipeId: Uuid, request: List<String>): List<Instruction> {
        return request.map { description ->
            val instructionId = Uuid.random()
            val createdAt = Instant.now()
            
            val instructionStatement = Instructions.insert {
                it[Instructions.id] = instructionId
                it[Instructions.recipe_id] = recipeId
                it[Instructions.best_version] = 1
                it[Instructions.created_at] = createdAt
            }

            InstructionDelta.insert {
                it[InstructionDelta.instruction_id] = instructionId
                it[InstructionDelta.version] = 1
                it[InstructionDelta.description] = description
                it[InstructionDelta.created_at] = createdAt
            }

            Instruction(
                id = instructionId,
                recipeId = recipeId,
                bestVersion = 1,
                notes = null,
                createdAt = instructionStatement[Instructions.created_at],
                description = description,
            )
        }
    }

    override suspend fun editRecipe(id: Uuid, request: EditRecipePayload): RecipeDetail {
        return transaction {
            val existing = Recipes
                .selectAll()
                .where { Recipes.id eq id }
                .singleOrNull() ?: throw NoSuchElementException("Recipe ${Recipes.id} not found")

            // Only touches Recipes table columns — ingredients/instructions are untouched here
            Recipes.update({ Recipes.id eq id }) {
                request.name?.let { name -> it[Recipes.name] = name }
                request.description?.let { description -> it[Recipes.description] = description }
                request.recipeSource?.let { source -> it[Recipes.recipe_source] = source }
                request.tags?.let { tags -> it[Recipes.tags] = tags }
                request.tools?.let { tools -> it[Recipes.tools] = tools }
            }

            val updatedRow = Recipes
                .selectAll()
                .where { Recipes.id eq id }
                .single()

            RecipeDetail(
                id = updatedRow[Recipes.id],
                name = updatedRow[Recipes.name],
                description = updatedRow[Recipes.description],
                createdAt = updatedRow[Recipes.created_at],
                recipeSource = updatedRow[Recipes.recipe_source],
                tags = updatedRow[Recipes.tags],
                tools = updatedRow[Recipes.tools],
                notes = updatedRow[Recipes.notes],
                ingredients = getIngredientsForRecipe(id),
                instructions = getInstructionsForRecipe(id),
            )
        }
    }

    override suspend fun editIngredient(ingredientId: Uuid, request: EditIngredientPayload): Ingredient {
        return transaction {
            val ingredientRow = Ingredients
                .selectAll()
                .where { Ingredients.id eq ingredientId }
                .singleOrNull() ?: throw NoSuchElementException("Ingredient $ingredientId not found")

            val maxVersionExpr = IngredientDelta.version.max()
            val highestVersion = IngredientDelta
                .select(maxVersionExpr)
                .where { IngredientDelta.ingredient_id eq ingredientId }
                .single()[maxVersionExpr] ?: 0

            val newVersion = highestVersion + 1
            val createdAt = Instant.now()

            IngredientDelta.insert {
                it[IngredientDelta.ingredient_id] = ingredientId
                it[IngredientDelta.version] = newVersion
                it[IngredientDelta.amount] = request.amount
                it[IngredientDelta.name] = request.name
                it[IngredientDelta.created_at] = createdAt
            }

            if (request.setAsBestVersion ?: false) {
                Ingredients.update({ Ingredients.id eq ingredientId }) {
                    it[Ingredients.best_version] = newVersion
                }
            }

            Ingredient(
                id = ingredientId,
                recipeId = ingredientRow[Ingredients.recipe_id],
                bestVersion = if (request.setAsBestVersion ?: false) newVersion else ingredientRow[Ingredients.best_version],
                notes = ingredientRow[Ingredients.notes],
                createdAt = ingredientRow[Ingredients.created_at],
                amount = request.amount,
                name = request.name,
            )
        }
    }

    override suspend fun editInstruction(instructionId: Uuid, request: EditInstructionPayload): Instruction {
        return transaction {
            val instructionRow = Instructions
                .selectAll()
                .where { Instructions.id eq instructionId }
                .singleOrNull() ?: throw NoSuchElementException("Instruction $instructionId not found")

            val maxVersionExpr = InstructionDelta.version.max()
            val highestVersion = InstructionDelta
                .select(maxVersionExpr)
                .where { InstructionDelta.instruction_id eq instructionId }
                .single()[maxVersionExpr] ?: 0

            val newVersion = highestVersion + 1
            val createdAt = Instant.now()

            InstructionDelta.insert {
                it[InstructionDelta.instruction_id] = instructionId
                it[InstructionDelta.version] = newVersion
                it[InstructionDelta.description] = description
                it[InstructionDelta.created_at] = createdAt
            }

            if (request.setAsBestVersion ?: false) {
                Instructions.update({ Instructions.id eq instructionId }) {
                    it[Instructions.best_version] = newVersion
                }
            }

            Instruction(
                id = instructionId,
                recipeId = instructionRow[Instructions.recipe_id],
                bestVersion = if (request.setAsBestVersion ?: false) newVersion else instructionRow[Instructions.best_version],
                notes = instructionRow[Instructions.notes],
                createdAt = instructionRow[Instructions.created_at],
                description = request.description,
            )
        }
    }

    override suspend fun updateRecipeNotes(recipeId: Uuid, notes: String?) {
        return transaction {
            Recipes
                .selectAll()
                .where { Recipes.id eq recipeId }
                .singleOrNull() ?: throw NoSuchElementException("Recipe $recipeId not found")

            val updatedRows = Recipes.update({ Recipes.id eq recipeId }) {
                it[Recipes.notes] = notes
            }
        }
    }

    override suspend fun updateIngredientNotes(ingredientId: Uuid, notes: String?) {
        return transaction {
            Ingredients
                .selectAll()
                .where { Ingredients.id eq ingredientId }
                .singleOrNull() ?: throw NoSuchElementException("Ingredient $ingredientId not found")
                
            val updatedRows = Ingredients.update({ Ingredients.id eq ingredientId }) {
                it[Ingredients.notes] = notes as String
            }
        }
    }

    override suspend fun updateInstructionNotes(instructionId: Uuid, notes: String?) {
        return transaction {
            Instructions
                .selectAll()
                .where { Instructions.id eq instructionId }
                .singleOrNull() ?: throw NoSuchElementException("Recipe $instructionId not found")
                
            val updatedRows = Instructions.update({ Instructions.id eq instructionId }) {
                it[Instructions.notes] = notes as String
            }
        }
    }
    
    override suspend fun deleteRecipe(id: Uuid): Boolean {
        return transaction {
            val existing = Recipes
                .selectAll()
                .where { Recipes.id eq id }
                .singleOrNull() ?: return@transaction false

            val ingredientIds = Ingredients
                .selectAll()
                .where { Ingredients.recipe_id eq id }
                .map { it[Ingredients.id] }

            val instructionIds = Instructions
                .selectAll()
                .where { Instructions.recipe_id eq id }
                .map { it[Instructions.id] }

            val bakeIds = Bakes
                .selectAll()
                .where { Bakes.recipe_id eq id }
                .map { it[Bakes.id] }

            if (bakeIds.isNotEmpty()) {
                BakeIngredients.deleteWhere { BakeIngredients.bake_id inList bakeIds }
                BakeInstructions.deleteWhere { BakeInstructions.bake_id inList bakeIds }
            }
            Bakes.deleteWhere { Bakes.recipe_id eq id }

            if (ingredientIds.isNotEmpty()) {
                IngredientDelta.deleteWhere { IngredientDelta.ingredient_id inList ingredientIds }
            }
            Ingredients.deleteWhere { Ingredients.recipe_id eq id }

            if (instructionIds.isNotEmpty()) {
                InstructionDelta.deleteWhere { InstructionDelta.instruction_id inList instructionIds }
            }
            Instructions.deleteWhere { Instructions.recipe_id eq id }

            Recipes.deleteWhere { Recipes.id eq id }

            true
        }
    }
}