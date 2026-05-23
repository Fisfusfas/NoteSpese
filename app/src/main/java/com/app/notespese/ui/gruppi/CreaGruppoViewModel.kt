package com.app.notespese.ui.gruppi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Gruppo
import com.app.notespese.data.repository.AuthRepository
import com.app.notespese.data.repository.GruppoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreaGruppoViewModel @Inject constructor(
    private val gruppoRepository: GruppoRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // ── Form state (mutableStateOf: aggiornamenti immediati nel Compose frame) ──
    var nome by mutableStateOf("")
        private set
    var descrizione by mutableStateOf("")
        private set
    var icona by mutableStateOf("wallet")
        private set
    var colore by mutableStateOf("#1565C0")
        private set

    val nomeValido: Boolean get() = nome.isNotBlank()
    var nomeToccato by mutableStateOf(false)
        private set

    fun aggiornaNome(v: String)        { nome = v }
    fun aggiornaDescrizione(v: String) { descrizione = v }
    fun aggiornaIcona(v: String)       { icona = v }
    fun aggiornaColore(v: String)      { colore = v }
    fun segnaomeToccato()              { nomeToccato = true }

    // ── Stato della creazione ─────────────────────────────────────────────────
    sealed interface StatoCreazione {
        data object Idle        : StatoCreazione
        data object Caricamento : StatoCreazione
        data class  Successo(val gruppoId: String) : StatoCreazione
        data class  Errore(val messaggio: String)  : StatoCreazione
    }

    private val _stato = MutableStateFlow<StatoCreazione>(StatoCreazione.Idle)
    val stato: StateFlow<StatoCreazione> = _stato.asStateFlow()

    fun creaGruppo() {
        if (!nomeValido) { nomeToccato = true; return }
        viewModelScope.launch {
            _stato.value = StatoCreazione.Caricamento
            val userId = authRepository.utenteCorrente.firstOrNull()?.id
                ?: run { _stato.value = StatoCreazione.Errore("Utente non autenticato"); return@launch }

            val gruppo = Gruppo(
                nome        = nome.trim(),
                descrizione = descrizione.trim(),
                icona       = icona,
                colore      = colore,
            )
            gruppoRepository.creaGruppo(gruppo, userId)
                .onSuccess { id -> _stato.value = StatoCreazione.Successo(id) }
                .onFailure { e  -> _stato.value = StatoCreazione.Errore(e.message ?: "Errore durante la creazione") }
        }
    }

    fun resetStato() { _stato.value = StatoCreazione.Idle }
}
