package com.app.notespese.data.repository

import com.app.notespese.data.model.MeseConfig
import com.app.notespese.data.model.ModalitaSplit
import com.app.notespese.data.model.Saldo
import kotlinx.coroutines.flow.Flow

interface SaldoRepository {

    fun osservaSaldi(gruppoId: String, meseId: String): Flow<List<Saldo>>

    fun osservaMeseConfig(gruppoId: String, meseId: String): Flow<MeseConfig?>

    /** Ricalcola e scrive i saldi per il mese usando il debt simplification algorithm. */
    suspend fun calcolaESalvaSaldi(gruppoId: String, meseId: String): Result<Unit>

    /** Salva la modalità di suddivisione per il mese (crea il documento se non esiste). */
    suspend fun impostaSplit(
        gruppoId: String,
        meseId: String,
        modalita: ModalitaSplit,
        pesi: Map<String, Double>,
    ): Result<Unit>

    /** Debitore segna come pagato. */
    suspend fun segnaComePagato(gruppoId: String, meseId: String, saldoId: String): Result<Unit>

    /** Creditore conferma la ricezione. */
    suspend fun confermaPagamento(gruppoId: String, meseId: String, saldoId: String): Result<Unit>
}
