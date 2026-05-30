package com.app.notespese.ui.gruppi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Gruppo
import com.app.notespese.data.repository.AuthRepository
import com.app.notespese.data.repository.GruppoRepository
import com.app.notespese.data.repository.InvitoRepository
import com.app.notespese.data.repository.WidgetPreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListaGruppiViewModel @Inject constructor(
    private val gruppoRepository: GruppoRepository,
    private val authRepository: AuthRepository,
    private val invitoRepository: InvitoRepository,
    private val widgetPrefs: WidgetPreferenceRepository,
) : ViewModel() {

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(val gruppi: List<Gruppo>) : UiState
        data class Errore(val messaggio: String) : UiState
    }

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

    // ── Auto-navigate a gruppo di default ────────────────────────────────────────

    private val _navigaToGruppo = MutableSharedFlow<String>(replay = 0)
    val navigaToGruppo: SharedFlow<String> = _navigaToGruppo

    init {
        viewModelScope.launch {
            combine(uiState, widgetPrefs.widgetGruppoId) { state, widgetId ->
                (state as? UiState.Successo)?.let { s ->
                    val gruppi = s.gruppi
                    when {
                        gruppi.size == 1             -> gruppi.first().id
                        widgetId.isNotBlank() && gruppi.any { it.id == widgetId } -> widgetId
                        else                         -> null
                    }
                }
            }
            .filterNotNull()
            .take(1)
            .collect { gruppoId -> _navigaToGruppo.emit(gruppoId) }
        }
    }

    // ── Accettazione invito ────────────────────────────────────────────────────

    var cercandoInvito  by mutableStateOf(false)
    var erroreInvito    by mutableStateOf<String?>(null)
    var invitoAccettato by mutableStateOf<String?>(null)

    fun entraConCodice(codice: String) {
        if (codice.isBlank()) return
        viewModelScope.launch {
            cercandoInvito = true
            erroreInvito   = null
            val userId = authRepository.utenteCorrente.first()?.id
            if (userId == null) {
                erroreInvito   = "Utente non autenticato"
                cercandoInvito = false
                return@launch
            }
            invitoRepository.trovaCodice(codice.uppercase().trim()).fold(
                onSuccess = { invito ->
                    if (invito == null) {
                        erroreInvito = "Codice non trovato o scaduto"
                    } else {
                        invitoRepository.accettaInvito(invito.id, userId).fold(
                            onSuccess = { invitoAccettato = invito.gruppoId },
                            onFailure = { erroreInvito = it.message ?: "Errore nell'accettare l'invito" },
                        )
                    }
                },
                onFailure = { erroreInvito = it.message ?: "Errore nella ricerca" },
            )
            cercandoInvito = false
        }
    }

    fun resetInvito() {
        erroreInvito    = null
        invitoAccettato = null
    }
}
