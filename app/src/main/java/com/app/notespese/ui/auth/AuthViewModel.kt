package com.app.notespese.ui.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.R
import com.app.notespese.data.model.Utente
import com.app.notespese.data.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    sealed interface UiState {
        data object Caricamento     : UiState
        data object NonAutenticato  : UiState
        data class  Autenticato(val utente: Utente) : UiState
        data class  Errore(val messaggio: String)   : UiState
    }

    /** Stato derivato dalla fonte di verità del repository (authStateListener di Firebase). */
    val uiState: StateFlow<UiState> = authRepository.utenteCorrente
        .map { utente ->
            if (utente != null) UiState.Autenticato(utente)
            else                UiState.NonAutenticato
        }
        .catch { e -> emit(UiState.Errore(e.message ?: "Errore sconosciuto")) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Caricamento
        )

    /** True durante il tentativo di sign-in (Credential Manager + Firebase). */
    private val _signInLoading = MutableStateFlow(false)
    val signInLoading: StateFlow<Boolean> = _signInLoading.asStateFlow()

    /** Messaggio di errore del sign-in. La UI lo consuma chiamando clearSignInError(). */
    private val _signInError = MutableStateFlow<String?>(null)
    val signInError: StateFlow<String?> = _signInError.asStateFlow()

    /**
     * Avvia il flusso Google Sign-In tramite Credential Manager.
     * Richiede l'Activity perché Credential Manager mostra un bottom sheet di sistema.
     *
     * Flusso:
     * 1. Mostra il selettore account Google (Credential Manager)
     * 2. Ottiene GoogleIdTokenCredential
     * 3. Passa l'ID token al repository → Firebase credential exchange
     */
    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _signInLoading.value = true
            _signInError.value   = null
            try {
                val credentialManager = CredentialManager.create(activity)
                val webClientId = activity.getString(R.string.google_web_client_id)

                // GetSignInWithGoogleOption mostra il selettore account OAuth classico.
                // Non può essere combinato con GetGoogleIdOption (One Tap).
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                val result     = credentialManager.getCredential(activity, request)
                val idToken    = GoogleIdTokenCredential.createFrom(result.credential.data).idToken

                authRepository.signIn(idToken).onFailure { e ->
                    _signInError.value = e.message ?: "Errore durante l'accesso"
                }
            } catch (e: GetCredentialCancellationException) {
                // L'utente ha chiuso il selettore: non è un errore da mostrare
            } catch (e: GetCredentialException) {
                _signInError.value = "Errore Google: ${e.message}"
            } catch (e: Exception) {
                _signInError.value = e.message ?: "Errore sconosciuto"
            } finally {
                _signInLoading.value = false
            }
        }
    }

    /**
     * Sign-in anonimo per la modalità demo.
     * Richiede "Accesso anonimo" abilitato su Firebase Console > Authentication > Sign-in method.
     */
    fun signInAnonymously() {
        viewModelScope.launch {
            _signInLoading.value = true
            _signInError.value   = null
            authRepository.signInAnonymously().onFailure { e ->
                _signInError.value = e.message ?: "Errore durante l'accesso demo"
            }
            _signInLoading.value = false
        }
    }

    fun clearSignInError() {
        _signInError.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
