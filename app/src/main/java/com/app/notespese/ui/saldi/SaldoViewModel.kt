package com.app.notespese.ui.saldi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.Saldo
import com.app.notespese.data.repository.AuthRepository
import com.app.notespese.data.repository.GruppoRepository
import com.app.notespese.data.repository.SaldoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class SaldoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val saldoRepository: SaldoRepository,
    gruppoRepository: GruppoRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(
            val nomeGruppo: String,
            val saldi: List<Saldo>,
            val membri: List<Membro>,
            val mese: Int,
            val anno: Int,
            val userId: String,
        ) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    sealed interface AzioneEsito {
        data object Inattivo    : AzioneEsito
        data object Caricamento : AzioneEsito
        data class  Errore(val msg: String) : AzioneEsito
    }

    private val now = YearMonth.now()
    private val _meseAnno = MutableStateFlow(now.monthValue to now.year)

    var azioneEsito by mutableStateOf<AzioneEsito>(AzioneEsito.Inattivo)

    val uiState: StateFlow<UiState> = _meseAnno
        .flatMapLatest { (mese, anno) ->
            val meseId = "%04d-%02d".format(anno, mese)
            combine(
                gruppoRepository.osservaGruppo(gruppoId),
                gruppoRepository.osservaMembri(gruppoId),
                saldoRepository.osservaSaldi(gruppoId, meseId),
                authRepository.utenteCorrente,
            ) { gruppo, membri, saldi, utente ->
                if (gruppo == null) UiState.Errore("Gruppo non trovato")
                else UiState.Successo(
                    nomeGruppo = gruppo.nome,
                    saldi      = saldi,
                    membri     = membri,
                    mese       = mese,
                    anno       = anno,
                    userId     = utente?.id ?: "",
                )
            }
        }
        .catch { e -> emit(UiState.Errore(e.message ?: "Errore sconosciuto")) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Caricamento,
        )

    fun mesePrecedente() {
        _meseAnno.value = _meseAnno.value.let { (m, a) ->
            val ym = YearMonth.of(a, m).minusMonths(1); ym.monthValue to ym.year
        }
    }

    fun meseSuccessivo() {
        _meseAnno.value = _meseAnno.value.let { (m, a) ->
            val ym = YearMonth.of(a, m).plusMonths(1); ym.monthValue to ym.year
        }
    }

    fun calcolaESalva() {
        val state  = uiState.value as? UiState.Successo ?: return
        val meseId = "%04d-%02d".format(state.anno, state.mese)
        viewModelScope.launch {
            azioneEsito = AzioneEsito.Caricamento
            azioneEsito = saldoRepository.calcolaESalvaSaldi(gruppoId, meseId).fold(
                onSuccess = { AzioneEsito.Inattivo },
                onFailure = { AzioneEsito.Errore(it.message ?: "Errore nel calcolo") },
            )
        }
    }

    fun segnaComePagato(saldoId: String) {
        val state  = uiState.value as? UiState.Successo ?: return
        val meseId = "%04d-%02d".format(state.anno, state.mese)
        viewModelScope.launch {
            azioneEsito = AzioneEsito.Caricamento
            azioneEsito = saldoRepository.segnaComePagato(gruppoId, meseId, saldoId).fold(
                onSuccess = { AzioneEsito.Inattivo },
                onFailure = { AzioneEsito.Errore(it.message ?: "Errore") },
            )
        }
    }

    fun confermaPagamento(saldoId: String) {
        val state  = uiState.value as? UiState.Successo ?: return
        val meseId = "%04d-%02d".format(state.anno, state.mese)
        viewModelScope.launch {
            azioneEsito = AzioneEsito.Caricamento
            azioneEsito = saldoRepository.confermaPagamento(gruppoId, meseId, saldoId).fold(
                onSuccess = { AzioneEsito.Inattivo },
                onFailure = { AzioneEsito.Errore(it.message ?: "Errore") },
            )
        }
    }
}
