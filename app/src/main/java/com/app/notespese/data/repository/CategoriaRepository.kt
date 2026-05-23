package com.app.notespese.data.repository

import com.app.notespese.data.model.Categoria
import kotlinx.coroutines.flow.Flow

interface CategoriaRepository {

    fun osservaCategorie(gruppoId: String): Flow<List<Categoria>>

    /** Crea la categoria se non esiste, ritorna l'id. */
    suspend fun aggiungiCategoria(gruppoId: String, categoria: Categoria): Result<String>

    suspend fun aggiornaCategoria(gruppoId: String, categoria: Categoria): Result<Unit>

    suspend fun eliminaCategoria(gruppoId: String, categoriaId: String): Result<Unit>
}
