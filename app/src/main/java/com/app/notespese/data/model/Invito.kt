package com.app.notespese.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/** Documento: inviti/{invitoId} — radice, non subcollection */
data class Invito(
    @DocumentId val id: String = "",
    val gruppoId: String = "",
    val gruppoNome: String = "",
    val creatoDa: String = "",
    val codice: String = "",
    val scadeIl: Timestamp? = null,
    val stato: String = StatoInvito.PENDING.name,
    val usatoDa: String? = null,
)
