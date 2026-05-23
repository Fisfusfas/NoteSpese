package com.app.notespese.data.repository

import com.app.notespese.data.model.Gruppo
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.Ruolo
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseGruppoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : GruppoRepository {

    private fun gruppiRef() = firestore.collection("gruppi")

    override fun osservaGruppiUtente(userId: String): Flow<List<Gruppo>> = callbackFlow {
        val listener = gruppiRef()
            .whereArrayContains("membroIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Gruppo::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun osservaGruppo(gruppoId: String): Flow<Gruppo?> = callbackFlow {
        val listener = gruppiRef().document(gruppoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObject(Gruppo::class.java))
            }
        awaitClose { listener.remove() }
    }

    override fun osservaMembri(gruppoId: String): Flow<List<Membro>> = callbackFlow {
        val listener = gruppiRef().document(gruppoId).collection("membri")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Membro::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    /**
     * Batch atomico:
     * 1. Crea gruppi/{id} con membroIds già popolato
     * 2. Crea gruppi/{id}/membri/{creatorId} con ruolo ADMIN
     */
    override suspend fun creaGruppo(gruppo: Gruppo, creatorId: String): Result<String> = runCatching {
        val docRef = gruppiRef().document()
        val membro = Membro(
            id              = creatorId,
            userId          = creatorId,
            ruolo           = Ruolo.ADMIN.name,
            nominativoLocale = "",
            aggiuntoIl      = Timestamp.now(),
            widgetDefault   = false,
        )
        val gruppoConId = gruppo.copy(
            id         = docRef.id,
            creatoDa   = creatorId,
            membroIds  = listOf(creatorId),
        )
        firestore.runBatch { batch ->
            batch.set(docRef, gruppoConId)
            batch.set(docRef.collection("membri").document(creatorId), membro)
        }.await()
        docRef.id
    }

    override suspend fun aggiornaGruppo(gruppo: Gruppo): Result<Unit> = runCatching {
        gruppiRef().document(gruppo.id).set(gruppo).await()
    }

    override suspend fun eliminaGruppo(gruppoId: String): Result<Unit> = runCatching {
        // Nota: elimina solo il documento radice. Le subcollection vanno eliminate
        // tramite Cloud Function in produzione (Firestore non elimina ricorsivamente lato client).
        gruppiRef().document(gruppoId).delete().await()
    }

    override suspend fun aggiungiMembro(gruppoId: String, membro: Membro): Result<Unit> = runCatching {
        firestore.runBatch { batch ->
            val gruppoRef = gruppiRef().document(gruppoId)
            val membroRef = gruppoRef.collection("membri").document(membro.userId)
            batch.set(membroRef, membro)
            // Aggiorna l'array membroIds nel documento gruppo
            batch.update(gruppoRef, "membroIds", FieldValue.arrayUnion(membro.userId))
        }.await()
    }

    override suspend fun rimuoviMembro(gruppoId: String, userId: String): Result<Unit> = runCatching {
        firestore.runBatch { batch ->
            val gruppoRef = gruppiRef().document(gruppoId)
            batch.delete(gruppoRef.collection("membri").document(userId))
            batch.update(gruppoRef, "membroIds", FieldValue.arrayRemove(userId))
        }.await()
    }

    override suspend fun aggiornaMembro(gruppoId: String, membro: Membro): Result<Unit> = runCatching {
        gruppiRef().document(gruppoId)
            .collection("membri")
            .document(membro.userId)
            .set(membro)
            .await()
    }
}
