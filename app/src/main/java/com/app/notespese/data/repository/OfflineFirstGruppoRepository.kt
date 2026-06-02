package com.app.notespese.data.repository

import com.app.notespese.data.local.dao.GruppoDao
import com.app.notespese.data.local.dao.MembroDao
import com.app.notespese.data.local.entity.GruppoEntity
import com.app.notespese.data.local.entity.MembroEntity
import com.app.notespese.data.local.entity.toEntity
import com.app.notespese.data.local.entity.toModel
import com.app.notespese.data.model.Gruppo
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.Ruolo
import com.app.notespese.di.ApplicationScope
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
class OfflineFirstGruppoRepository @Inject constructor(
    private val gruppoDao: GruppoDao,
    private val membroDao: MembroDao,
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val appScope: CoroutineScope,
) : GruppoRepository {

    // key = userId
    private val gruppiListeners = ConcurrentHashMap<String, ListenerRegistration>()

    // key = gruppoId
    private val membriListeners = ConcurrentHashMap<String, ListenerRegistration>()

    private fun ensureSyncGruppi(userId: String) {
        gruppiListeners.computeIfAbsent(userId) {
            firestore.collection("gruppi")
                .whereArrayContains("membroIds", userId)
                .addSnapshotListener { snapshot, _ ->
                    snapshot ?: return@addSnapshotListener
                    appScope.launch {
                        val entities = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Gruppo::class.java)?.copy(id = doc.id)?.toEntity()
                        }
                        gruppoDao.syncAll(userId, entities)
                    }
                }
        }
    }

    private fun ensureSyncMembri(gruppoId: String) {
        membriListeners.computeIfAbsent(gruppoId) {
            firestore.collection("gruppi").document(gruppoId)
                .collection("membri")
                .addSnapshotListener { snapshot, _ ->
                    snapshot ?: return@addSnapshotListener
                    appScope.launch {
                        val entities = snapshot.documents.mapNotNull { doc ->
                            // In Firestore, il document ID del membro == userId
                            doc.toObject(Membro::class.java)?.copy(id = doc.id, userId = doc.id)?.toEntity(gruppoId)
                        }
                        membroDao.syncAll(gruppoId, entities)
                    }
                }
        }
    }

    override fun osservaGruppiUtente(userId: String): Flow<List<Gruppo>> {
        ensureSyncGruppi(userId)
        return gruppoDao.osservaGruppiUtente(userId).map { entities ->
            // Filtro preciso: LIKE potrebbe avere falsi positivi su substring, verifica esatta sul split
            entities.filter { entity ->
                entity.membroIdsStr.split("|").any { uid -> uid == userId }
            }.map { it.toModel() }
        }
    }

    override fun osservaGruppo(gruppoId: String): Flow<Gruppo?> =
        gruppoDao.osservaGruppo(gruppoId).map { it?.toModel() }

    override fun osservaMembri(gruppoId: String): Flow<List<Membro>> {
        ensureSyncMembri(gruppoId)
        return membroDao.osservaMembri(gruppoId).map { it.map(MembroEntity::toModel) }
    }

    override suspend fun creaGruppo(gruppo: Gruppo, creatorId: String): Result<String> {
        return runCatching {
            val docRef = firestore.collection("gruppi").document()
            val id = docRef.id
            val gruppoConId = gruppo.copy(id = id, membroIds = listOf(creatorId))
            val membroAdmin = Membro(
                id = creatorId,
                userId = creatorId,
                ruolo = Ruolo.ADMIN.name,
            )
            firestore.runBatch { batch ->
                batch.set(docRef, gruppoConId)
                batch.set(docRef.collection("membri").document(creatorId), membroAdmin)
            }.await()
            gruppoDao.upsert(gruppoConId.toEntity())
            membroDao.upsert(membroAdmin.toEntity(id))
            id
        }
    }

    override suspend fun aggiornaGruppo(gruppo: Gruppo): Result<Unit> {
        gruppoDao.upsert(gruppo.toEntity())
        return runCatching {
            // .update() invece di .set() per non sovrascrivere membroIds,
            // che viene gestito esclusivamente da aggiungiMembro/rimuoviMembro.
            firestore.collection("gruppi").document(gruppo.id).update(
                mapOf(
                    "nome"                 to gruppo.nome,
                    "descrizione"          to gruppo.descrizione,
                    "icona"                to gruppo.icona,
                    "colore"               to gruppo.colore,
                    "modalitaSplitDefault" to gruppo.modalitaSplitDefault,
                    "giornoInizioMese"     to gruppo.giornoInizioMese,
                )
            ).await()
        }
    }

    override suspend fun eliminaGruppo(gruppoId: String): Result<Unit> {
        gruppoDao.deleteById(gruppoId)
        return runCatching {
            firestore.collection("gruppi").document(gruppoId).delete().await()
        }
    }

    override suspend fun aggiungiMembro(gruppoId: String, membro: Membro): Result<Unit> {
        membroDao.upsert(membro.toEntity(gruppoId))
        return runCatching {
            firestore.collection("gruppi").document(gruppoId)
                .collection("membri").document(membro.userId).set(membro).await()
        }
    }

    override suspend fun rimuoviMembro(gruppoId: String, userId: String): Result<Unit> {
        membroDao.deleteByUserId(gruppoId, userId)
        return runCatching {
            firestore.collection("gruppi").document(gruppoId)
                .collection("membri").document(userId).delete().await()
        }
    }

    override suspend fun aggiornaMembro(gruppoId: String, membro: Membro): Result<Unit> {
        membroDao.upsert(membro.toEntity(gruppoId))
        return runCatching {
            firestore.collection("gruppi").document(gruppoId)
                .collection("membri").document(membro.userId).set(membro).await()
        }
    }

    override suspend fun aggiornaNominativoInTuttiGruppi(userId: String, nominativo: String): Result<Unit> {
        return runCatching {
            val gruppi = firestore.collection("gruppi")
                .whereArrayContains("membroIds", userId)
                .get().await()
            firestore.runBatch { batch ->
                gruppi.documents.forEach { doc ->
                    batch.update(
                        doc.reference.collection("membri").document(userId),
                        "nominativoLocale", nominativo,
                    )
                }
            }.await()
        }
    }

    fun clearListeners() {
        gruppiListeners.values.forEach { it.remove() }
        gruppiListeners.clear()
        membriListeners.values.forEach { it.remove() }
        membriListeners.clear()
    }
}
