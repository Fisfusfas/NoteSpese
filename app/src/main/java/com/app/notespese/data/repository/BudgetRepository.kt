package com.app.notespese.data.repository

import com.app.notespese.data.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun osservaBudget(gruppoId: String): Flow<List<Budget>>
    suspend fun impostaBudget(gruppoId: String, budget: Budget): Result<Unit>
    suspend fun eliminaBudget(gruppoId: String, categoriaId: String): Result<Unit>
}
