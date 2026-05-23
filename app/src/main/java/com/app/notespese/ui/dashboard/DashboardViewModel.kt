package com.app.notespese.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Gruppo
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.Spesa
import com.app.notespese.data.repository.GruppoRepository
import com.app.notespese.data.repository.SpesaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gruppoRepository: GruppoRepository,
    private val spesaRepository: SpesaRepository,
) : ViewModel() {

    private val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(
            val gruppo: Gruppo,
            val membri: List<Membro>,
            val speseDelMese: List<Spesa>,
            val mese: Int,
            val anno: Int,
        ) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    private val now = YearMonth.now()
    private val _meseAnno = MutableStateFlow(now.monthValue to now.year)

    val uiState: StateFlow<UiState> = _meseAnno
        .flatMapLatest { (mese, anno) ->
            combine(
                gruppoRepository.osservaGruppo(gruppoId),
                gruppoRepository.osservaMembri(gruppoId),
                spesaRepository.osservaSpesePerMese(gruppoId, mese, anno),
            ) { gruppo, membri, spese ->
                if (gruppo == null) UiState.Errore("Gruppo non trovato")
                else UiState.Successo(gruppo, membri, spese, mese, anno)
            }
        }
        .catch { e -> emit(UiState.Errore(e.message ?: "Errore sconosciuto")) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Caricamento
        )

    fun mesePrecedente() {
        _meseAnno.value = _meseAnno.value.let { (m, a) ->
            val ym = YearMonth.of(a, m).minusMonths(1)
            ym.monthValue to ym.year
        }
    }

    fun meseSuccessivo() {
        _meseAnno.value = _meseAnno.value.let { (m, a) ->
            val ym = YearMonth.of(a, m).plusMonths(1)
            ym.monthValue to ym.year
        }
    }
}
