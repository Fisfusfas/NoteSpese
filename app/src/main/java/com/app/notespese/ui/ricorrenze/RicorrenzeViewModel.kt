package com.app.notespese.ui.ricorrenze

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Categoria
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.Ricorrenza
import com.app.notespese.data.repository.CategoriaRepository
import com.app.notespese.data.repository.GruppoRepository
import com.app.notespese.data.repository.RicorrenzaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RicorrenzeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ricorrenzaRepository: RicorrenzaRepository,
    categoriaRepository: CategoriaRepository,
    gruppoRepository: GruppoRepository,
) : ViewModel() {

    val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(
            val ricorrenze: List<Ricorrenza>,
            val categorie: List<Categoria>,
            val membri: List<Membro>,
        ) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    val uiState: StateFlow<UiState> = combine(
        ricorrenzaRepository.osservaRicorrenze(gruppoId),
        categoriaRepository.osservaCategorie(gruppoId),
        gruppoRepository.osservaMembri(gruppoId),
    ) { ricorrenze, categorie, membri ->
        UiState.Successo(ricorrenze.sortedBy { it.descrizione }, categorie, membri) as UiState
    }
    .catch { e -> emit(UiState.Errore(e.message ?: "Errore")) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Caricamento)

    fun elimina(ricorrenzaId: String) {
        viewModelScope.launch { ricorrenzaRepository.eliminaRicorrenza(gruppoId, ricorrenzaId) }
    }

    fun toggleAttiva(ricorrenza: Ricorrenza) {
        viewModelScope.launch {
            ricorrenzaRepository.aggiornaRicorrenza(gruppoId, ricorrenza.copy(attiva = !ricorrenza.attiva))
        }
    }
}
