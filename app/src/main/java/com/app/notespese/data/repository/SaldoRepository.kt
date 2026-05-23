package com.app.notespese.data.repository

import com.app.notespese.data.model.Saldo
import kotlinx.coroutines.flow.Flow

interface SaldoRepository {

    fun osservaSaldi(gruppoId: String, meseId: String): Flow<List<Saldo>>

    /** Ricalcola e scrive i saldi per il mese usando il debt simplification algorithm. */
    suspend fun calcolaESalvaSaldi(gruppoId: String, meseId: String): Result<Unit>

    /** Debitore segna come pagato. */
    suspend fun segnaComePagato(gruppoId: String, meseId: String, saldoId: String): Result<Unit>

    /** Creditore conferma la ricezione. */
    suspend fun confermaPagamento(gruppoId: String, meseId: String, saldoId: String): Result<Unit>
}
