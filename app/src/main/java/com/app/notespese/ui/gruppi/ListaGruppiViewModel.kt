package com.app.notespese.ui.gruppi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Gruppo
import com.app.notespese.data.repository.AuthRepository
import com.app.notespese.data.repository.GruppoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ListaGruppiViewModel @Inject constructor(
    private val gruppoRepository: GruppoRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(val gruppi: List<Gruppo>) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    /**
     * flatMapLatest: quando l'utente cambia (login/logout), cancella il listener
     * Firestore precedente e ne apre uno nuovo per il nuovo userId.
     */
    val uiState: StateFlow<UiState> = authRepository.utenteCorrente
        .flatMapLatest { utente ->
            if (utente == null) flowOf(UiState.Successo(emptyList()))
            else gruppoRepository.osservaGruppiUtente(utente.id)
                .map { UiState.Successo(it) as UiState }
                .catch { e -> emit(UiState.Errore(e.message ?: "Errore")) }
        }
        .catch { e -> emit(UiState.Errore(e.message ?: "Errore")) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Caricamento
        )
}
