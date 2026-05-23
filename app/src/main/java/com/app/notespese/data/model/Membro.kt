package com.app.notespese.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Documento: gruppi/{gruppoId}/membri/{userId}
 *
 * [userId] duplica l'id documento per permettere collectionGroup queries
 * (FieldPath.documentId() non è filtrabile in collectionGroup con whereEqualTo).
 */
data class Membro(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ruolo: String = Ruolo.MEMBRO.name,
    val nominativoLocale: String = "",
    val aggiuntoIl: Timestamp? = null,
    val widgetDefault: Boolean = false,
)
