package com.app.notespese.data.repository

import com.app.notespese.data.model.Debito
import kotlinx.coroutines.flow.Flow

interface DebitoRepository {

    fun osservaDebiti(gruppoId: String): Flow<List<Debito>>

    suspend fun aggiungiDebito(gruppoId: String, debito: Debito): Result<String>

    suspend fun aggiornaDebito(gruppoId: String, debito: Debito): Result<Unit>

    suspend fun eliminaDebito(gruppoId: String, debitoId: String): Result<Unit>

    suspend fun segnaComeSaldato(gruppoId: String, debitoId: String): Result<Unit>
}
