package com.app.notespese.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.app.notespese.data.local.entity.MembroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MembroDao {

    @Query("SELECT * FROM membri WHERE gruppoId = :gruppoId ORDER BY nominativoLocale ASC")
    fun osservaMembri(gruppoId: String): Flow<List<MembroEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(membri: List<MembroEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(membro: MembroEntity)

    @Query("DELETE FROM membri WHERE gruppoId = :gruppoId AND userId = :userId")
    suspend fun deleteByUserId(gruppoId: String, userId: String)

    @Transaction
    suspend fun syncAll(gruppoId: String, entities: List<MembroEntity>) {
        upsertAll(entities)
        val userIds = entities.map { it.userId }.ifEmpty { listOf("__noop__") }
        deleteNotIn(gruppoId, userIds)
    }

    @Query("DELETE FROM membri WHERE gruppoId = :gruppoId AND userId NOT IN (:userIds)")
    suspend fun deleteNotIn(gruppoId: String, userIds: List<String>)
}
