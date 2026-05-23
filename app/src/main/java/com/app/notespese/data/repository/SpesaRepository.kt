package com.app.notespese.data.repository

import com.app.notespese.data.model.Spesa
import kotlinx.coroutines.flow.Flow

interface SpesaRepository {

    fun osservaSpese(gruppoId: String): Flow<List<Spesa>>

    fun osservaSpesePerMese(gruppoId: String, mese: Int, anno: Int): Flow<List<Spesa>>

    /** Ritorna l'id della spesa creata. */
    suspend fun aggiungiSpesa(gruppoId: String, spesa: Spesa): Result<String>

    suspend fun aggiornaSpesa(gruppoId: String, spesa: Spesa): Result<Unit>

    suspend fun eliminaSpesa(gruppoId: String, spesaId: String): Result<Unit>
}
