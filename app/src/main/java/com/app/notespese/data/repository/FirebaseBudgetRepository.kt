package com.app.notespese.data.repository

import com.app.notespese.data.model.Budget
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseBudgetRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : BudgetRepository {

    private fun ref(gruppoId: String) =
        firestore.collection("gruppi").document(gruppoId).collection("budget")

    override fun osservaBudget(gruppoId: String): Flow<List<Budget>> = callbackFlow {
        val listener = ref(gruppoId).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snapshot?.toObjects(Budget::class.java) ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    override suspend fun impostaBudget(gruppoId: String, budget: Budget): Result<Unit> = runCatching {
        ref(gruppoId).document(budget.id).set(budget).await()
    }

    override suspend fun eliminaBudget(gruppoId: String, categoriaId: String): Result<Unit> = runCatching {
        ref(gruppoId).document(categoriaId).delete().await()
    }
}
