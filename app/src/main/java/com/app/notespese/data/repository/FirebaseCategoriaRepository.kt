package com.app.notespese.data.repository

import com.app.notespese.data.model.Categoria
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCategoriaRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : CategoriaRepository {

    private fun categorieRef(gruppoId: String) =
        firestore.collection("gruppi").document(gruppoId).collection("categorie")

    override fun osservaCategorie(gruppoId: String): Flow<List<Categoria>> = callbackFlow {
        val listener = categorieRef(gruppoId)
            .orderBy("nome")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Categoria::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun aggiungiCategoria(gruppoId: String, categoria: Categoria): Result<String> = runCatching {
        val docRef = categorieRef(gruppoId).document()
        docRef.set(categoria.copy(id = docRef.id)).await()
        docRef.id
    }

    override suspend fun aggiornaCategoria(gruppoId: String, categoria: Categoria): Result<Unit> = runCatching {
        categorieRef(gruppoId).document(categoria.id).set(categoria).await()
    }

    override suspend fun eliminaCategoria(gruppoId: String, categoriaId: String): Result<Unit> = runCatching {
        categorieRef(gruppoId).document(categoriaId).delete().await()
    }
}
