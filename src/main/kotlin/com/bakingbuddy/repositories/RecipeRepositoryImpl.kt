package com.bakingbuddy.repositories

import com.bakingbuddy.database.IngredientDelta
import com.bakingbuddy.database.Ingredients
import com.bakingbuddy.database.InstructionDelta
import com.bakingbuddy.database.Instructions
import com.bakingbuddy.database.Recipes
import com.bakingbuddy.models.CreateIngredientPayload
import com.bakingbuddy.models.CreateRecipePayload
import com.bakingbuddy.models.Ingredient
import com.bakingbuddy.models.Instruction
import com.bakingbuddy.models.Recipe
import com.bakingbuddy.models.RecipeDetail
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

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
        return transaction {
            val recipeStatement = Recipes.insert {
                it[Recipes.name] = request.name
                it[Recipes.description] = request.description
                it[Recipes.recipe_source] = request.recipeSource.orEmpty()
                it[Recipes.tags] = request.tags.orEmpty()
                it[Recipes.tools] = request.tools.orEmpty()
            }

            val recipeId = recipeStatement[Recipes.id]
            val createdAt = recipeStatement[Recipes.created_at]

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
            )
        }
    }

    fun createIngredients(recipeId: Uuid, request: List<CreateIngredientPayload>): List<Ingredient> {
        return request.map { ingredient ->
            val ingredientStatement = Ingredients.insert {
                it[Ingredients.recipe_id] = recipeId
                it[Ingredients.best_version] = 1
            }
            val ingredientId = ingredientStatement[Ingredients.id]

            IngredientDelta.insert {
                it[IngredientDelta.ingredient_id] = ingredientId
                it[IngredientDelta.version] = 1
                it[IngredientDelta.amount] = ingredient.amount
                it[IngredientDelta.name] = ingredient.name
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
            val instructionStatement = Instructions.insert {
                it[Instructions.recipe_id] = recipeId
                it[Instructions.best_version] = 1
            }
            val instructionId = instructionStatement[Instructions.id]

            InstructionDelta.insert {
                it[InstructionDelta.instruction_id] = instructionId
                it[InstructionDelta.version] = 1
                it[InstructionDelta.description] = description
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
}