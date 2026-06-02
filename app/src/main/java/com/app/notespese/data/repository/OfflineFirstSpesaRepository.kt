package com.app.notespese.data.repository

import com.app.notespese.data.local.dao.SpesaDao
import com.app.notespese.data.local.entity.SpesaEntity
import com.app.notespese.data.local.entity.toEntity
import com.app.notespese.data.local.entity.toModel
import com.app.notespese.data.model.Spesa
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
class OfflineFirstSpesaRepository @Inject constructor(
    private val spesaDao: SpesaDao,
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val appScope: CoroutineScope,
) : SpesaRepository {

    private val listeners = ConcurrentHashMap<String, ListenerRegistration>()

    private fun ensureSync(gruppoId: String) {
        listeners.computeIfAbsent(gruppoId) {
            firestore.collection("gruppi").document(gruppoId)
                .collection("spese")
                .addSnapshotListener { snapshot, _ ->
                    snapshot ?: return@addSnapshotListener
                    appScope.launch {
                        val entities = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Spesa::class.java)?.copy(id = doc.id)?.toEntity(gruppoId)
                        }
                        spesaDao.syncAll(gruppoId, entities)
                    }
                }
        }
    }

    override fun osservaSpese(gruppoId: String): Flow<List<Spesa>> {
        ensureSync(gruppoId)
        return spesaDao.osservaSpese(gruppoId).map { it.map(SpesaEntity::toModel) }
    }

    override fun osservaSpesePerMese(gruppoId: String, mese: Int, anno: Int): Flow<List<Spesa>> {
        ensureSync(gruppoId)
        return spesaDao.osservaSpesePerMese(gruppoId, mese, anno).map { it.map(SpesaEntity::toModel) }
    }

    override fun osservaSpesePerPeriodo(gruppoId: String, start: Timestamp, end: Timestamp): Flow<List<Spesa>> {
        ensureSync(gruppoId)
        return spesaDao.osservaSpesePerPeriodo(
            gruppoId,
            start.toDate().time,
            end.toDate().time,
        ).map { it.map(SpesaEntity::toModel) }
    }

    override suspend fun aggiungiSpesa(gruppoId: String, spesa: Spesa): Result<String> {
        val docRef = firestore.collection("gruppi").document(gruppoId).collection("spese").document()
        val id = docRef.id
        val spesaConId = spesa.copy(id = id)
        spesaDao.upsert(spesaConId.toEntity(gruppoId))
        return runCatching {
            docRef.set(spesaConId).await()
            id
        }
    }

    override suspend fun aggiornaSpesa(gruppoId: String, spesa: Spesa): Result<Unit> {
        spesaDao.upsert(spesa.toEntity(gruppoId))
        return runCatching {
            firestore.collection("gruppi").document(gruppoId)
                .collection("spese").document(spesa.id).set(spesa).await()
        }
    }

    override suspend fun eliminaSpesa(gruppoId: String, spesaId: String): Result<Unit> {
        spesaDao.deleteById(spesaId)
        return runCatching {
            firestore.collection("gruppi").document(gruppoId)
                .collection("spese").document(spesaId).delete().await()
        }
    }

    override suspend fun getSpesa(gruppoId: String, spesaId: String): Result<Spesa?> {
        spesaDao.getById(spesaId)?.let { return Result.success(it.toModel()) }
        return runCatching {
            firestore.collection("gruppi").document(gruppoId)
                .collection("spese").document(spesaId)
                .get().await().toObject(Spesa::class.java)
        }
    }

    fun clearListeners() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
    }
}
