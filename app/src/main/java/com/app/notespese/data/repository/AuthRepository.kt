package com.app.notespese.data.repository

import com.app.notespese.data.model.Utente
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Emette l'utente corrente o null se non autenticato. Fonte di verità per lo stato auth. */
    val utenteCorrente: Flow<Utente?>

    /** Sign-in con Google tramite Credential Manager. [googleIdToken] proviene da GoogleIdTokenCredential. */
    suspend fun signIn(googleIdToken: String): Result<Utente>

    /** Sign-in anonimo (modalità demo / offline testing). */
    suspend fun signInAnonymously(): Result<Utente>

    suspend fun signOut()
}
