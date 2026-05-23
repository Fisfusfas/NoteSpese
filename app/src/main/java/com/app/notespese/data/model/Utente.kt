package com.app.notespese.data.model

data class Utente(
    val id: String,
    val nome: String,
    val email: String,
    val fotoUrl: String? = null,
    val fcmToken: String? = null
)
