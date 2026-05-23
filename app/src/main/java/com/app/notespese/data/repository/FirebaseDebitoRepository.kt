package com.app.notespese.data.repository

import com.app.notespese.data.model.Debito
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDebitoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : DebitoRepository {

    private fun debitiRef(gruppoId: String) =
        firestore.collection("gruppi").document(gruppoId).collection("debiti")

    override fun osservaDebiti(gruppoId: String): Flow<List<Debito>> = callbackFlow {
        val listener = debitiRef(gruppoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Debito::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun aggiungiDebito(gruppoId: String, debito: Debito): Result<String> = runCatching {
        val docRef = debitiRef(gruppoId).document()
        docRef.set(debito.copy(id = docRef.id)).await()
        docRef.id
    }

    override suspend fun aggiornaDebito(gruppoId: String, debito: Debito): Result<Unit> = runCatching {
        debitiRef(gruppoId).document(debito.id).set(debito).await()
    }

    override suspend fun eliminaDebito(gruppoId: String, debitoId: String): Result<Unit> = runCatching {
        debitiRef(gruppoId).document(debitoId).delete().await()
    }

    override suspend fun segnaComeSaldato(gruppoId: String, debitoId: String): Result<Unit> = runCatching {
        debitiRef(gruppoId).document(debitoId).update("saldato", true).await()
    }
}
