package com.app.notespese.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.app.notespese.data.local.entity.CategoriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Query("SELECT * FROM categorie WHERE gruppoId = :gruppoId ORDER BY nome ASC")
    fun osservaCategorie(gruppoId: String): Flow<List<CategoriaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categorie: List<CategoriaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(categoria: CategoriaEntity)

    @Query("DELETE FROM categorie WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun syncAll(gruppoId: String, entities: List<CategoriaEntity>) {
        upsertAll(entities)
        val ids = entities.map { it.id }.ifEmpty { listOf("__noop__") }
        deleteNotIn(gruppoId, ids)
    }

    @Query("DELETE FROM categorie WHERE gruppoId = :gruppoId AND id NOT IN (:ids)")
    suspend fun deleteNotIn(gruppoId: String, ids: List<String>)
}
