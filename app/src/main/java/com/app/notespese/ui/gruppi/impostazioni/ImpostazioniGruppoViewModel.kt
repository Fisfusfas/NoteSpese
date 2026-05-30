package com.app.notespese.ui.gruppi.impostazioni

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Gruppo
import com.app.notespese.data.model.Membro
import com.app.notespese.data.repository.AuthRepository
import com.app.notespese.data.repository.GruppoRepository
import com.app.notespese.data.repository.InvitoRepository
import com.app.notespese.data.repository.WidgetPreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImpostazioniGruppoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gruppoRepository: GruppoRepository,
    private val invitoRepository: InvitoRepository,
    authRepository: AuthRepository,
    private val widgetPrefs: WidgetPreferenceRepository,
) : ViewModel() {

    private val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(
            val nomeGruppo: String,
            val membri: List<Membro>,
            val userId: String,
            val widgetSelezionato: Boolean,
            val giornoInizioMese: Int,
        ) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    private var _gruppoCorrente: Gruppo? = null

    var invitoCodice      by mutableStateOf<String?>(null)
    var erroreInvito      by mutableStateOf<String?>(null)
    var generando         by mutableStateOf(false)
    var showEditNome      by mutableStateOf(false)
    var nuovoNome         by mutableStateOf("")
    var salvandoNome      by mutableStateOf(false)

    val uiState: StateFlow<UiState> = combine(
        gruppoRepository.osservaGruppo(gruppoId),
        gruppoRepository.osservaMembri(gruppoId),
        authRepository.utenteCorrente,
        widgetPrefs.widgetGruppoId,
    ) { gruppo, membri, utente, widgetGruppoId ->
        _gruppoCorrente = gruppo
        if (gruppo == null) UiState.Errore("Gruppo non trovato")
        else UiState.Successo(
            nomeGruppo        = gruppo.nome,
            membri            = membri,
            userId            = utente?.id ?: "",
            widgetSelezionato = widgetGruppoId == gruppoId,
            giornoInizioMese  = gruppo.giornoInizioMese,
        )
    }
    .catch { e -> emit(UiState.Errore(e.message ?: "Errore")) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Caricamento)

    fun salvaGiornoInizioMese(giorno: Int) {
        val gruppo = _gruppoCorrente ?: return
        viewModelScope.launch {
            gruppoRepository.aggiornaGruppo(gruppo.copy(giornoInizioMese = giorno.coerceIn(1, 28)))
        }
    }

    fun generaInvito() {
        val state = uiState.value as? UiState.Successo ?: return
        generando = true
        viewModelScope.launch {
            invitoRepository.creaInvito(gruppoId, state.nomeGruppo, state.userId).fold(
                onSuccess = { invito ->
                    invitoCodice = invito.codice
                    erroreInvito = null
                },
                onFailure = { erroreInvito = it.message ?: "Errore nella generazione" },
            )
            generando = false
        }
    }

    fun chiudiDialogCodice() { invitoCodice = null }

    fun apriEditNome() {
        val state = uiState.value as? UiState.Successo ?: return
        val membro = state.membri.find { it.userId == state.userId }
        nuovoNome = membro?.nominativoLocale ?: ""
        showEditNome = true
    }

    fun salvaNome() {
        val state = uiState.value as? UiState.Successo ?: return
        val membro = state.membri.find { it.userId == state.userId } ?: return
        salvandoNome = true
        viewModelScope.launch {
            gruppoRepository.aggiornaMembro(gruppoId, membro.copy(nominativoLocale = nuovoNome.trim()))
            salvandoNome = false
            showEditNome = false
        }
    }

    fun toggleWidget() {
        val state = uiState.value as? UiState.Successo ?: return
        viewModelScope.launch {
            widgetPrefs.setWidgetGruppoId(if (state.widgetSelezionato) "" else gruppoId)
        }
    }
}
