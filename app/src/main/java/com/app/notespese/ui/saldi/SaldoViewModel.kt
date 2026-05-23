package com.app.notespese.ui.saldi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.MeseConfig
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.ModalitaSplit
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
            val meseConfig: MeseConfig?,
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

    // ── Dialog split ───────────────────────────────────────────────────────────
    var showSplitDialog by mutableStateOf(false)
    var splitModalita   by mutableStateOf(ModalitaSplit.CINQUANTA)
    var splitPesi       by mutableStateOf<Map<String, String>>(emptyMap())

    val uiState: StateFlow<UiState> = _meseAnno
        .flatMapLatest { (mese, anno) ->
            val meseId = "%04d-%02d".format(anno, mese)
            combine(
                gruppoRepository.osservaGruppo(gruppoId),
                gruppoRepository.osservaMembri(gruppoId),
                saldoRepository.osservaSaldi(gruppoId, meseId),
                authRepository.utenteCorrente,
                saldoRepository.osservaMeseConfig(gruppoId, meseId),
            ) { gruppo, membri, saldi, utente, meseConfig ->
                if (gruppo == null) UiState.Errore("Gruppo non trovato")
                else UiState.Successo(
                    nomeGruppo = gruppo.nome,
                    saldi      = saldi,
                    membri     = membri,
                    mese       = mese,
                    anno       = anno,
                    userId     = utente?.id ?: "",
                    meseConfig = meseConfig,
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

    fun apriFialogSplit() {
        val state  = uiState.value as? UiState.Successo ?: return
        val config = state.meseConfig
        splitModalita = ModalitaSplit.entries.find { it.name == config?.modalitaSplit }
            ?: ModalitaSplit.CINQUANTA
        splitPesi = state.membri.associate { m ->
            val peso = config?.splitPersonalizzato?.get(m.userId) ?: 1.0
            m.userId to formatPeso(peso)
        }
        showSplitDialog = true
    }

    fun chiudiDialogSplit() { showSplitDialog = false }

    fun salvaSplit() {
        val state  = uiState.value as? UiState.Successo ?: return
        val meseId = "%04d-%02d".format(state.anno, state.mese)
        // DA_ENTRATE e CINQUANTA non richiedono pesi manuali
        val pesi   = when (splitModalita) {
            ModalitaSplit.CINQUANTA, ModalitaSplit.DA_ENTRATE -> emptyMap()
            else -> splitPesi.mapValues { it.value.toDoubleOrNull() ?: 1.0 }
        }
        showSplitDialog = false
        viewModelScope.launch {
            azioneEsito = AzioneEsito.Caricamento
            saldoRepository.impostaSplit(gruppoId, meseId, splitModalita, pesi).fold(
                onSuccess = { calcolaESalva() },
                onFailure = { azioneEsito = AzioneEsito.Errore(it.message ?: "Errore nel salvataggio") },
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

    private fun formatPeso(v: Double): String =
        if (v == kotlin.math.floor(v) && !v.isInfinite()) v.toLong().toString() else v.toString()
}
