package com.app.notespese.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.app.notespese.data.local.entity.SpesaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpesaDao {

    @Query("SELECT * FROM spese WHERE gruppoId = :gruppoId ORDER BY dataMillis DESC")
    fun osservaSpese(gruppoId: String): Flow<List<SpesaEntity>>

    @Query("SELECT * FROM spese WHERE gruppoId = :gruppoId AND mese = :mese AND anno = :anno ORDER BY dataMillis DESC")
    fun osservaSpesePerMese(gruppoId: String, mese: Int, anno: Int): Flow<List<SpesaEntity>>

    @Query("SELECT * FROM spese WHERE gruppoId = :gruppoId AND dataMillis >= :startMillis AND dataMillis < :endMillis ORDER BY dataMillis DESC")
    fun osservaSpesePerPeriodo(gruppoId: String, startMillis: Long, endMillis: Long): Flow<List<SpesaEntity>>

    @Query("SELECT * FROM spese WHERE id = :id")
    suspend fun getById(id: String): SpesaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(spese: List<SpesaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(spesa: SpesaEntity)

    @Query("DELETE FROM spese WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun syncAll(gruppoId: String, entities: List<SpesaEntity>) {
        upsertAll(entities)
        val ids = entities.map { it.id }.ifEmpty { listOf("__noop__") }
        deleteNotIn(gruppoId, ids)
    }

    @Query("DELETE FROM spese WHERE gruppoId = :gruppoId AND id NOT IN (:ids)")
    suspend fun deleteNotIn(gruppoId: String, ids: List<String>)
}
