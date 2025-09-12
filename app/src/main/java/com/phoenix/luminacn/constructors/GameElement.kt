
package com.phoenix.luminacn.constructors

data class GameElement(
    val name: String,
    val category: Category? = null,
    val isEnabled: Boolean = false,
    val priority: Int? = null
)

data class Category(
    val name: String
)