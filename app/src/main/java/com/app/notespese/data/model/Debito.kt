package com.app.notespese.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/** Documento: gruppi/{gruppoId}/debiti/{debitoId} */
data class Debito(
    @DocumentId val id: String = "",
    val importo: Double = 0.0,
    val tipo: String = TipoDebito.PRESTITO_FATTO.name,
    val controparte: String = "",
    val persona: String = "",
    val data: Timestamp? = null,
    val scadenza: Timestamp? = null,
    val saldato: Boolean = false,
    val note: String = "",
)
