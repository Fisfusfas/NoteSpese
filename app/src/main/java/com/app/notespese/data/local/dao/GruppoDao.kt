package com.app.notespese.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.app.notespese.data.local.entity.GruppoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GruppoDao {

    // Avvolge membroIdsStr con '|' su entrambi i lati per garantire boundary matching esatto.
    // Es: "uid1|uid2" → "|uid1|uid2|" LIKE "%|uid1|%" → match corretto senza falsi positivi.
    // Il filtro aggiuntivo nel repository (split+any) è comunque mantenuto per sicurezza.
    @Query("SELECT * FROM gruppi WHERE ('|' || membroIdsStr || '|') LIKE ('%|' || :userId || '|%')")
    fun osservaGruppiUtente(userId: String): Flow<List<GruppoEntity>>

    @Query("SELECT * FROM gruppi WHERE id = :gruppoId")
    fun osservaGruppo(gruppoId: String): Flow<GruppoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(gruppo: GruppoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(gruppi: List<GruppoEntity>)

    @Query("DELETE FROM gruppi WHERE id = :gruppoId")
    suspend fun deleteById(gruppoId: String)

    @Transaction
    suspend fun syncAll(userId: String, entities: List<GruppoEntity>) {
        upsertAll(entities)
        val ids = entities.map { it.id }.ifEmpty { listOf("__noop__") }
        deleteNotInForUser(userId, ids)
    }

    @Query("DELETE FROM gruppi WHERE ('|' || membroIdsStr || '|') LIKE ('%|' || :userId || '|%') AND id NOT IN (:ids)")
    suspend fun deleteNotInForUser(userId: String, ids: List<String>)
}
