package com.app.notespese.data.repository

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordina il cleanup dei listener Firestore di tutti i repository offline-first.
 * Da chiamare al logout per evitare che listener del vecchio account continuino ad operare.
 */
@Singleton
class LocalSyncCoordinator @Inject constructor(
    private val gruppoRepo: OfflineFirstGruppoRepository,
    private val spesaRepo: OfflineFirstSpesaRepository,
    private val entrataRepo: OfflineFirstEntrataRepository,
    private val categoriaRepo: OfflineFirstCategoriaRepository,
) {
    fun clearAll() {
        gruppoRepo.clearListeners()
        spesaRepo.clearListeners()
        entrataRepo.clearListeners()
        categoriaRepo.clearListeners()
    }
}
