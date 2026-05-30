package com.app.notespese.data.repository

import com.app.notespese.data.model.Spesa
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSpesaRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : SpesaRepository {

    private fun speseRef(gruppoId: String) =
        firestore.collection("gruppi").document(gruppoId).collection("spese")

    override fun osservaSpese(gruppoId: String): Flow<List<Spesa>> = callbackFlow {
        val listener = speseRef(gruppoId)
            .orderBy("data", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Spesa::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun osservaSpesePerMese(gruppoId: String, mese: Int, anno: Int): Flow<List<Spesa>> = callbackFlow {
        val listener = speseRef(gruppoId)
            .whereEqualTo("anno", anno)
            .whereEqualTo("mese", mese)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Spesa::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun osservaSpesePerPeriodo(gruppoId: String, start: Timestamp, end: Timestamp): Flow<List<Spesa>> = callbackFlow {
        val listener = speseRef(gruppoId)
            .whereGreaterThanOrEqualTo("data", start)
            .whereLessThan("data", end)
            .orderBy("data", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Spesa::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun aggiungiSpesa(gruppoId: String, spesa: Spesa): Result<String> = runCatching {
        val docRef = speseRef(gruppoId).document()
        docRef.set(spesa.copy(id = docRef.id)).await()
        docRef.id
    }

    override suspend fun aggiornaSpesa(gruppoId: String, spesa: Spesa): Result<Unit> = runCatching {
        speseRef(gruppoId).document(spesa.id).set(spesa).await()
    }

    override suspend fun eliminaSpesa(gruppoId: String, spesaId: String): Result<Unit> = runCatching {
        speseRef(gruppoId).document(spesaId).delete().await()
    }

    override suspend fun getSpesa(gruppoId: String, spesaId: String): Result<Spesa?> = runCatching {
        speseRef(gruppoId).document(spesaId).get().await().toObject(Spesa::class.java)
    }
}
