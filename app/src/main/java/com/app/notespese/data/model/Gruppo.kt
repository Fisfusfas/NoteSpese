package com.app.notespese.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Documento: gruppi/{gruppoId}
 *
 * [membroIds] è ridondante rispetto alla subcollection membri, ma serve per
 * whereArrayContains("membroIds", userId) — query O(1) senza collectionGroup.
 */
data class Gruppo(
    @DocumentId val id: String = "",
    val nome: String = "",
    val descrizione: String = "",
    val creatoDa: String = "",
    val icona: String = "wallet",
    val colore: String = "#1565C0",
    val modalitaSplitDefault: String = ModalitaSplit.COEFFICIENTE.name,
    val membroIds: List<String> = emptyList(),
    val giornoInizioMese: Int = 1,
)
