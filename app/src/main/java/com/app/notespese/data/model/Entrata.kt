package com.app.notespese.data.model

import com.google.firebase.firestore.DocumentId

/** Documento: gruppi/{gruppoId}/entrate/{entrataId} */
data class Entrata(
    @DocumentId val id: String = "",
    val importo: Double = 0.0,
    val persona: String = "",
    val categoriaId: String = "",
    val mese: Int = 0,
    val anno: Int = 0,
    val note: String = "",
)
