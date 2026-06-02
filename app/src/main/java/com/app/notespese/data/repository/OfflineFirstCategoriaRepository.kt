package com.app.notespese.data.repository

import com.app.notespese.data.local.dao.CategoriaDao
import com.app.notespese.data.local.entity.CategoriaEntity
import com.app.notespese.data.local.entity.toEntity
import com.app.notespese.data.local.entity.toModel
import com.app.notespese.data.model.Categoria
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
class OfflineFirstCategoriaRepository @Inject constructor(
    private val categoriaDao: CategoriaDao,
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val appScope: CoroutineScope,
) : CategoriaRepository {

    private val listeners = ConcurrentHashMap<String, ListenerRegistration>()

    private fun ensureSync(gruppoId: String) {
        listeners.computeIfAbsent(gruppoId) {
            firestore.collection("gruppi").document(gruppoId)
                .collection("categorie")
                .addSnapshotListener { snapshot, _ ->
                    snapshot ?: return@addSnapshotListener
                    appScope.launch {
                        val entities = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Categoria::class.java)?.copy(id = doc.id)?.toEntity(gruppoId)
                        }
                        categoriaDao.syncAll(gruppoId, entities)
                    }
                }
        }
    }

    override fun osservaCategorie(gruppoId: String): Flow<List<Categoria>> {
        ensureSync(gruppoId)
        return categoriaDao.osservaCategorie(gruppoId).map { it.map(CategoriaEntity::toModel) }
    }

    override suspend fun aggiungiCategoria(gruppoId: String, categoria: Categoria): Result<String> {
        val docRef = firestore.collection("gruppi").document(gruppoId).collection("categorie").document()
        val id = docRef.id
        val catConId = categoria.copy(id = id)
        categoriaDao.upsert(catConId.toEntity(gruppoId))
        return runCatching {
            docRef.set(catConId).await()
            id
        }
    }

    override suspend fun aggiornaCategoria(gruppoId: String, categoria: Categoria): Result<Unit> {
        categoriaDao.upsert(categoria.toEntity(gruppoId))
        return runCatching {
            firestore.collection("gruppi").document(gruppoId)
                .collection("categorie").document(categoria.id).set(categoria).await()
        }
    }

    override suspend fun eliminaCategoria(gruppoId: String, categoriaId: String): Result<Unit> {
        categoriaDao.deleteById(categoriaId)
        return runCatching {
            firestore.collection("gruppi").document(gruppoId)
                .collection("categorie").document(categoriaId).delete().await()
        }
    }

    fun clearListeners() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
    }
}
