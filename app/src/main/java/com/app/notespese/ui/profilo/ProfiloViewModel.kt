package com.app.notespese.ui.profilo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Utente
import com.app.notespese.data.repository.AuthRepository
import com.app.notespese.data.repository.GruppoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfiloViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gruppoRepository: GruppoRepository,
) : ViewModel() {

    val utente: StateFlow<Utente?> = authRepository.utenteCorrente
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var editNickname  by mutableStateOf("")
    var salvando      by mutableStateOf(false)
    var errore        by mutableStateOf<String?>(null)
    var salvato       by mutableStateOf(false)

    fun inizializza(nome: String) {
        if (editNickname.isEmpty()) editNickname = nome
    }

    fun salva() {
        val nome = editNickname.trim()
        if (nome.isBlank()) { errore = "Il nome non può essere vuoto"; return }
        val userId = utente.value?.id ?: return
        salvando = true
        errore   = null
        salvato  = false
        viewModelScope.launch {
            val r1 = authRepository.aggiornaNome(nome)
            val r2 = gruppoRepository.aggiornaNominativoInTuttiGruppi(userId, nome)
            salvando = false
            val err = r1.exceptionOrNull() ?: r2.exceptionOrNull()
            if (err != null) {
                errore = err.message ?: "Errore nel salvataggio"
            } else {
                salvato = true
            }
        }
    }
}
