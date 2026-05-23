package com.app.notespese.data.repository

import com.app.notespese.data.model.Entrata
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseEntrataRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : EntrataRepository {

    private fun entrateRef(gruppoId: String) =
        firestore.collection("gruppi").document(gruppoId).collection("entrate")

    override fun osservaEntrate(gruppoId: String): Flow<List<Entrata>> = callbackFlow {
        val listener = entrateRef(gruppoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Entrata::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun osservaEntratePerMese(gruppoId: String, mese: Int, anno: Int): Flow<List<Entrata>> = callbackFlow {
        val listener = entrateRef(gruppoId)
            .whereEqualTo("anno", anno)
            .whereEqualTo("mese", mese)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Entrata::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun aggiungiEntrata(gruppoId: String, entrata: Entrata): Result<String> = runCatching {
        val docRef = entrateRef(gruppoId).document()
        docRef.set(entrata.copy(id = docRef.id)).await()
        docRef.id
    }

    override suspend fun aggiornaEntrata(gruppoId: String, entrata: Entrata): Result<Unit> = runCatching {
        entrateRef(gruppoId).document(entrata.id).set(entrata).await()
    }

    override suspend fun eliminaEntrata(gruppoId: String, entrataId: String): Result<Unit> = runCatching {
        entrateRef(gruppoId).document(entrataId).delete().await()
    }
}
