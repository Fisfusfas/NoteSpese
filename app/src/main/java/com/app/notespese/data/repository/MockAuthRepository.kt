package com.app.notespese.data.repository

import com.app.notespese.data.model.Utente
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAuthRepository @Inject constructor() : AuthRepository {

    private val _utente = MutableStateFlow<Utente?>(null)

    override val utenteCorrente: Flow<Utente?> = _utente

    override suspend fun signIn(googleIdToken: String): Result<Utente> {
        val demo = Utente(
            id     = "mock-google-uid",
            nome   = "Utente Google Demo",
            email  = "google@notespese.dev",
            fotoUrl = null
        )
        _utente.value = demo
        return Result.success(demo)
    }

    override suspend fun signInAnonymously(): Result<Utente> {
        val demo = Utente(
            id     = "mock-anon-uid",
            nome   = "Utente Demo",
            email  = "",
            fotoUrl = null
        )
        _utente.value = demo
        return Result.success(demo)
    }

    override suspend fun signOut() {
        _utente.value = null
    }

    override suspend fun aggiornaNome(nome: String): Result<Unit> {
        _utente.value = _utente.value?.copy(nome = nome)
        return Result.success(Unit)
    }
}
