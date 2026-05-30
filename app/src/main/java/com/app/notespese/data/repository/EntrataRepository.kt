package com.app.notespese.data.repository

import com.app.notespese.data.model.Entrata
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

interface EntrataRepository {

    fun osservaEntrate(gruppoId: String): Flow<List<Entrata>>

    fun osservaEntratePerMese(gruppoId: String, mese: Int, anno: Int): Flow<List<Entrata>>

    fun osservaEntratePerPeriodo(gruppoId: String, start: Timestamp, end: Timestamp): Flow<List<Entrata>>

    suspend fun aggiungiEntrata(gruppoId: String, entrata: Entrata): Result<String>

    suspend fun aggiornaEntrata(gruppoId: String, entrata: Entrata): Result<Unit>

    suspend fun eliminaEntrata(gruppoId: String, entrataId: String): Result<Unit>

    suspend fun getEntrata(gruppoId: String, entrataId: String): Result<Entrata?>
}
