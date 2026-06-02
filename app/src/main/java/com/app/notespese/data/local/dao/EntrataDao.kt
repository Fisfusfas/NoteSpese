package com.app.notespese.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.app.notespese.data.local.entity.EntrataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntrataDao {

    @Query("SELECT * FROM entrate WHERE gruppoId = :gruppoId ORDER BY dataMillis DESC")
    fun osservaEntrate(gruppoId: String): Flow<List<EntrataEntity>>

    @Query("SELECT * FROM entrate WHERE gruppoId = :gruppoId AND mese = :mese AND anno = :anno ORDER BY dataMillis DESC")
    fun osservaEntratePerMese(gruppoId: String, mese: Int, anno: Int): Flow<List<EntrataEntity>>

    @Query("SELECT * FROM entrate WHERE gruppoId = :gruppoId AND dataMillis >= :startMillis AND dataMillis < :endMillis ORDER BY dataMillis DESC")
    fun osservaEntratePerPeriodo(gruppoId: String, startMillis: Long, endMillis: Long): Flow<List<EntrataEntity>>

    @Query("SELECT * FROM entrate WHERE id = :id")
    suspend fun getById(id: String): EntrataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entrate: List<EntrataEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entrata: EntrataEntity)

    @Query("DELETE FROM entrate WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun syncAll(gruppoId: String, entities: List<EntrataEntity>) {
        upsertAll(entities)
        val ids = entities.map { it.id }.ifEmpty { listOf("__noop__") }
        deleteNotIn(gruppoId, ids)
    }

    @Query("DELETE FROM entrate WHERE gruppoId = :gruppoId AND id NOT IN (:ids)")
    suspend fun deleteNotIn(gruppoId: String, ids: List<String>)
}
