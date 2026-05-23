package com.app.notespese.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Documento: gruppi/{gruppoId}/mesi/{meseId}  es. "2026-05"
 *
 * Creato automaticamente al primo accesso al mese, o manualmente.
 * [splitPersonalizzato] è usato solo quando [modalitaSplit] == PERSONALIZZATO.
 */
data class MeseConfig(
    @DocumentId val id: String = "",
    val modalitaSplit: String = ModalitaSplit.COEFFICIENTE.name,
    val splitPersonalizzato: Map<String, Double> = emptyMap(),
    val chiusoManualmente: Boolean = false,
    val chiusoDa: String = "",
    val chiusoIl: Timestamp? = null,
)
