package com.app.notespese.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Documento: gruppi/{gruppoId}/budget/{categoriaId}
 * L'id documento coincide con il categoriaId.
 */
data class Budget(
    @DocumentId val id: String = "",
    val importoMensile: Double = 0.0,
    val notifica80: Boolean = true,
    val notifica100: Boolean = true,
)
