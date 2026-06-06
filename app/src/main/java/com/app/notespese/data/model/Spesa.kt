package com.app.notespese.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

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
) {
    // Campo locale: non viene mai scritto su Firestore (escluso dalla serializzazione).
    // Viene impostato dal repository in base a DocumentSnapshot.metadata.hasPendingWrites.
    @get:Exclude
    var pendingWrite: Boolean = false
}
