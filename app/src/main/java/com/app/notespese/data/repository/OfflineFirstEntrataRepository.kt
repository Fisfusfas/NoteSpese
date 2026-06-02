package com.app.notespese.data.repository

import com.app.notespese.data.local.dao.EntrataDao
import com.app.notespese.data.local.entity.EntrataEntity
import com.app.notespese.data.local.entity.toEntity
import com.app.notespese.data.local.entity.toModel
import com.app.notespese.data.model.Entrata
import com.app.notespese.di.ApplicationScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstEntrataRepository @Inject constructor(
    private val entrataDao: EntrataDao,
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val appScope: CoroutineScope,
) : EntrataRepository {

    private val listeners = ConcurrentHashMap<String, ListenerRegistration>()

    private fun ensureSync(gruppoId: String) {
        listeners.computeIfAbsent(gruppoId) {
            firestore.collection("gruppi").document(gruppoId)
                .collection("entrate")
                .addSnapshotListener { snapshot, _ ->
                    snapshot ?: return@addSnapshotListener
                    appScope.launch {
                        val entities = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Entrata::class.java)?.copy(id = doc.id)?.toEntity(gruppoId)
                        }
                        entrataDao.syncAll(gruppoId, entities)
                    }
                }
        }
    }

    override fun osservaEntrate(gruppoId: String): Flow<List<Entrata>> {
        ensureSync(gruppoId)
        return entrataDao.osservaEntrate(gruppoId).map { it.map(EntrataEntity::toModel) }
    }

    override fun osservaEntratePerMese(gruppoId: String, mese: Int, anno: Int): Flow<List<Entrata>> {
        ensureSync(gruppoId)
        return entrataDao.osservaEntratePerMese(gruppoId, mese, anno).map { it.map(EntrataEntity::toModel) }
    }

    override fun osservaEntratePerPeriodo(gruppoId: String, start: Timestamp, end: Timestamp): Flow<List<Entrata>> {
        ensureSync(gruppoId)
        return entrataDao.osservaEntratePerPeriodo(
            gruppoId,
            start.toDate().time,
            end.toDate().time,
        ).map { it.map(EntrataEntity::toModel) }
    }

    override suspend fun aggiungiEntrata(gruppoId: String, entrata: Entrata): Result<String> {
        val docRef = firestore.collection("gruppi").document(gruppoId).collection("entrate").document()
        val id = docRef.id
        val entrataConId = entrata.copy(id = id)
        entrataDao.upsert(entrataConId.toEntity(gruppoId))
        return runCatching {
            docRef.set(entrataConId).await()
            id
        }
    }

    override suspend fun aggiornaEntrata(gruppoId: String, entrata: Entrata): Result<Unit> {
        entrataDao.upsert(entrata.toEntity(gruppoId))
        return runCatching {
            firestore.collection("gruppi").document(gruppoId)
                .collection("entrate").document(entrata.id).set(entrata).await()
        }
    }

    override suspend fun eliminaEntrata(gruppoId: String, entrataId: String): Result<Unit> {
        entrataDao.deleteById(entrataId)
        return runCatching {
            firestore.collection("gruppi").document(gruppoId)
                .collection("entrate").document(entrataId).delete().await()
        }
    }

    override suspend fun getEntrata(gruppoId: String, entrataId: String): Result<Entrata?> {
        entrataDao.getById(entrataId)?.let { return Result.success(it.toModel()) }
        return runCatching {
            firestore.collection("gruppi").document(gruppoId)
                .collection("entrate").document(entrataId)
                .get().await().toObject(Entrata::class.java)
        }
    }

    fun clearListeners() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
    }
}
