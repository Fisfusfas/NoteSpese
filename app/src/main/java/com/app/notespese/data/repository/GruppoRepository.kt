package com.app.notespese.data.repository

import com.app.notespese.data.model.Gruppo
import com.app.notespese.data.model.Membro
import kotlinx.coroutines.flow.Flow

interface GruppoRepository {

    /** Real-time: tutti i gruppi a cui appartiene [userId]. */
    fun osservaGruppiUtente(userId: String): Flow<List<Gruppo>>

    /** Real-time: dati di un singolo gruppo. */
    fun osservaGruppo(gruppoId: String): Flow<Gruppo?>

    /** Real-time: lista dei membri di un gruppo. */
    fun osservaMembri(gruppoId: String): Flow<List<Membro>>

    /**
     * Crea il gruppo su Firestore in batch:
     * - documento gruppi/{id}
     * - subcollection membri/{creatorId} con ruolo ADMIN
     * Ritorna l'id del gruppo creato.
     */
    suspend fun creaGruppo(gruppo: Gruppo, creatorId: String): Result<String>

    suspend fun aggiornaGruppo(gruppo: Gruppo): Result<Unit>

    suspend fun eliminaGruppo(gruppoId: String): Result<Unit>

    suspend fun aggiungiMembro(gruppoId: String, membro: Membro): Result<Unit>

    suspend fun rimuoviMembro(gruppoId: String, userId: String): Result<Unit>

    suspend fun aggiornaMembro(gruppoId: String, membro: Membro): Result<Unit>
}
