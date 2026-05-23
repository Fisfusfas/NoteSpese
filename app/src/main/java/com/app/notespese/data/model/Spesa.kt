package com.app.notespese.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Documento: gruppi/{gruppoId}/spese/{spesaId}
 *
 * [mese] e [anno] sono denormalizzati dalla [data] per permettere filtri semplici
 * su Firestore senza composite index su Timestamp range.
 */
data class Spesa(
    @DocumentId val id: String = "",
    val importo: Double = 0.0,
    val descrizione: String = "",
    val categoriaId: String = "",
    val pagante: String = "",
    val condivisa: Boolean = true,
    val tipo: String = TipoSpesa.VARIABILE.name,
    val data: Timestamp? = null,
    val mese: Int = 0,
    val anno: Int = 0,
    val note: String = "",
)
