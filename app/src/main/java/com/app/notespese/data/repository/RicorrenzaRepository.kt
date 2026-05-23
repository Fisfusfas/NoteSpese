package com.app.notespese.data.repository

import com.app.notespese.data.model.Ricorrenza
import kotlinx.coroutines.flow.Flow

interface RicorrenzaRepository {
    fun osservaRicorrenze(gruppoId: String): Flow<List<Ricorrenza>>
    suspend fun aggiungiRicorrenza(gruppoId: String, ricorrenza: Ricorrenza): Result<String>
    suspend fun aggiornaRicorrenza(gruppoId: String, ricorrenza: Ricorrenza): Result<Unit>
    suspend fun eliminaRicorrenza(gruppoId: String, ricorrenzaId: String): Result<Unit>
    suspend fun getRicorrenza(gruppoId: String, ricorrenzaId: String): Result<Ricorrenza?>
}
