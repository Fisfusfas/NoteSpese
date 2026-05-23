package com.app.notespese.data.model

import com.google.firebase.firestore.DocumentId

/** Documento: gruppi/{gruppoId}/categorie/{categoriaId} */
data class Categoria(
    @DocumentId val id: String = "",
    val nome: String = "",
    val icona: String = "label",
    val colore: String = "#1565C0",
    val tipo: String = TipoCategoria.ENTRAMBI.name,
)
