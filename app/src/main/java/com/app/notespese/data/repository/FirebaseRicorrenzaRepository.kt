package com.app.notespese.data.repository

import com.app.notespese.data.model.Ricorrenza
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRicorrenzaRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : RicorrenzaRepository {

    private fun ref(gruppoId: String) =
        firestore.collection("gruppi").document(gruppoId).collection("ricorrenze")

    override fun osservaRicorrenze(gruppoId: String): Flow<List<Ricorrenza>> = callbackFlow {
        val listener = ref(gruppoId).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snapshot?.toObjects(Ricorrenza::class.java) ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    override suspend fun aggiungiRicorrenza(gruppoId: String, ricorrenza: Ricorrenza): Result<String> = runCatching {
        val docRef = ref(gruppoId).document()
        docRef.set(ricorrenza.copy(id = docRef.id)).await()
        docRef.id
    }

    override suspend fun aggiornaRicorrenza(gruppoId: String, ricorrenza: Ricorrenza): Result<Unit> = runCatching {
        ref(gruppoId).document(ricorrenza.id).set(ricorrenza).await()
    }

    override suspend fun eliminaRicorrenza(gruppoId: String, ricorrenzaId: String): Result<Unit> = runCatching {
        ref(gruppoId).document(ricorrenzaId).delete().await()
    }

    override suspend fun getRicorrenza(gruppoId: String, ricorrenzaId: String): Result<Ricorrenza?> = runCatching {
        ref(gruppoId).document(ricorrenzaId).get().await().toObject(Ricorrenza::class.java)
    }
}
