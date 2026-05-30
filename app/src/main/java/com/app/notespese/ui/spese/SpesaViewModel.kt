package com.app.notespese.ui.spese

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Categoria
import com.app.notespese.data.model.Spesa
import com.app.notespese.data.repository.CategoriaRepository
import com.app.notespese.data.repository.GruppoRepository
import com.app.notespese.data.repository.SpesaRepository
import com.app.notespese.util.calcolaPeriodo
import com.app.notespese.util.etichettaPeriodo
import com.app.notespese.util.toTimestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class SpesaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val spesaRepository: SpesaRepository,
    private val gruppoRepository: GruppoRepository,
    categoriaRepository: CategoriaRepository,
) : ViewModel() {

    val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(
            val nomeGruppo: String,
            val spese: List<Spesa>,
            val categorie: List<Categoria>,
            val periodoLabel: String,
            val mese: Int,
            val anno: Int,
        ) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    private val now = YearMonth.now()
    private val _meseAnno = MutableStateFlow(now.monthValue to now.year)

    private val categorieFlow = categoriaRepository.osservaCategorie(gruppoId)

    val uiState: StateFlow<UiState> = _meseAnno
        .flatMapLatest { (mese, anno) ->
            gruppoRepository.osservaGruppo(gruppoId).flatMapLatest { gruppo ->
                if (gruppo == null) return@flatMapLatest flowOf(UiState.Errore("Gruppo non trovato"))
                val giornoInizio = gruppo.giornoInizioMese
                val speseFlow = if (giornoInizio <= 1) {
                    spesaRepository.osservaSpesePerMese(gruppoId, mese, anno)
                } else {
                    val (start, end) = calcolaPeriodo(giornoInizio, mese, anno)
                    spesaRepository.osservaSpesePerPeriodo(
                        gruppoId,
                        toTimestamp(start),
                        toTimestamp(end.plusDays(1))
                    )
                }
                combine(speseFlow, categorieFlow) { spese, categorie ->
                    UiState.Successo(
                        nomeGruppo   = gruppo.nome,
                        spese        = spese.sortedByDescending { it.data?.seconds ?: 0L },
                        categorie    = categorie,
                        periodoLabel = etichettaPeriodo(giornoInizio, mese, anno),
                        mese         = mese,
                        anno         = anno,
                    )
                }
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
            val ym = YearMonth.of(a, m).minusMonths(1); ym.monthValue to ym.year
        }
    }

    fun meseSuccessivo() {
        _meseAnno.value = _meseAnno.value.let { (m, a) ->
            val ym = YearMonth.of(a, m).plusMonths(1); ym.monthValue to ym.year
        }
    }

    fun setMese(mese: Int, anno: Int) {
        if (_meseAnno.value != mese to anno) _meseAnno.value = mese to anno
    }

    fun tornaAdOggi() {
        val now = YearMonth.now()
        _meseAnno.value = now.monthValue to now.year
    }

    fun eliminaSpesa(spesaId: String) {
        viewModelScope.launch { spesaRepository.eliminaSpesa(gruppoId, spesaId) }
    }
}
