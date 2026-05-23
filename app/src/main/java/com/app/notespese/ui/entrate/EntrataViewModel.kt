package com.app.notespese.ui.entrate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Categoria
import com.app.notespese.data.model.Entrata
import com.app.notespese.data.model.Membro
import com.app.notespese.data.repository.CategoriaRepository
import com.app.notespese.data.repository.EntrataRepository
import com.app.notespese.data.repository.GruppoRepository
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
class EntrataViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val entrataRepository: EntrataRepository,
    gruppoRepository: GruppoRepository,
    categoriaRepository: CategoriaRepository,
) : ViewModel() {

    val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(
            val nomeGruppo: String,
            val entrate: List<Entrata>,
            val categorie: List<Categoria>,
            val membri: List<Membro>,
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
                entrataRepository.osservaEntratePerMese(gruppoId, mese, anno),
                categoriaRepository.osservaCategorie(gruppoId),
                gruppoRepository.osservaMembri(gruppoId),
            ) { gruppo, entrate, categorie, membri ->
                if (gruppo == null) UiState.Errore("Gruppo non trovato")
                else UiState.Successo(
                    nomeGruppo = gruppo.nome,
                    entrate    = entrate,
                    categorie  = categorie,
                    membri     = membri,
                    mese       = mese,
                    anno       = anno,
                )
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

    fun eliminaEntrata(entrataId: String) {
        viewModelScope.launch { entrataRepository.eliminaEntrata(gruppoId, entrataId) }
    }
}
