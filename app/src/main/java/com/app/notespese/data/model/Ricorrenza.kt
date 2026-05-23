package com.app.notespese.data.model

import com.google.firebase.firestore.DocumentId

/** Documento: gruppi/{gruppoId}/ricorrenze/{ricorrenzaId} */
data class Ricorrenza(
    @DocumentId val id: String = "",
    val importo: Double = 0.0,
    val descrizione: String = "",
    val categoriaId: String = "",
    val tipo: String = TipoSpesa.FISSA.name,
    val condivisa: Boolean = true,
    val pagante: String = "",
    val giornoDelMese: Int = 1,
    val attiva: Boolean = true,
)
