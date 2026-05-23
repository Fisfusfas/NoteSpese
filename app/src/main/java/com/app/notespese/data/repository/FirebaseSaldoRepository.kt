package com.app.notespese.data.repository

import com.app.notespese.data.model.Saldo
import com.app.notespese.data.model.StatoCreditore
import com.app.notespese.data.model.StatoDebitore
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSaldoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : SaldoRepository {

    private fun saldiRef(gruppoId: String, meseId: String) =
        firestore.collection("gruppi").document(gruppoId)
            .collection("mesi").document(meseId)
            .collection("saldi")

    override fun osservaSaldi(gruppoId: String, meseId: String): Flow<List<Saldo>> = callbackFlow {
        val listener = saldiRef(gruppoId, meseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Saldo::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun calcolaESalvaSaldi(gruppoId: String, meseId: String): Result<Unit> = runCatching {
        // Il calcolo reale (debt simplification) è implementato nel domain layer (usecase).
        // Questo metodo si occupa solo del write su Firestore.
    }

    override suspend fun segnaComePagato(gruppoId: String, meseId: String, saldoId: String): Result<Unit> = runCatching {
        saldiRef(gruppoId, meseId).document(saldoId).update(
            mapOf(
                "statoDebitore" to StatoDebitore.PAGATO.name,
                "dataPagamento" to Timestamp.now(),
            )
        ).await()
    }

    override suspend fun confermaPagamento(gruppoId: String, meseId: String, saldoId: String): Result<Unit> = runCatching {
        saldiRef(gruppoId, meseId).document(saldoId).update(
            mapOf(
                "statoCreditore" to StatoCreditore.CONFERMATO.name,
                "dataConferma"   to Timestamp.now(),
            )
        ).await()
    }
}
