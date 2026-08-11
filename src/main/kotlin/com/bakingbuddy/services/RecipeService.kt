package com.bakingbuddy.services

import com.bakingbuddy.models.Recipe

class RecipeService {

    fun getRecipe(id: Int): Recipe? {
        return if (id == 1) {
            Recipe(
                id = 1,
                name = "Chocolate Chip Cookies",
                description = "Classic chewy chocolate chip cookies"
            )
        } else {
            null
        }
    }
}