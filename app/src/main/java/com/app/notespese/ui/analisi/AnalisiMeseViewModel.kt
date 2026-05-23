package com.app.notespese.ui.analisi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.repository.CategoriaRepository
import com.app.notespese.data.repository.SpesaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AnalisiMeseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    spesaRepository: SpesaRepository,
    categoriaRepository: CategoriaRepository,
) : ViewModel() {

    val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])
    val mese: Int        = checkNotNull(savedStateHandle["mese"])
    val anno: Int        = checkNotNull(savedStateHandle["anno"])

    data class CategoriaAnalisi(
        val categoriaId: String,
        val nome: String,
        val colore: String,
        val totale: Double,
        val nSpese: Int,
        val percentuale: Float,
    )

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(
            val totale: Double,
            val perCategoria: List<CategoriaAnalisi>,
        ) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    val uiState: StateFlow<UiState> = combine(
        spesaRepository.osservaSpesePerMese(gruppoId, mese, anno),
        categoriaRepository.osservaCategorie(gruppoId),
    ) { spese, categorie ->
        val totale = spese.sumOf { it.importo }
        val perCat = spese
            .groupBy { it.categoriaId }
            .map { (catId, gruppo) ->
                val cat      = categorie.find { it.id == catId }
                val subtotal = gruppo.sumOf { it.importo }
                CategoriaAnalisi(
                    categoriaId = catId,
                    nome        = cat?.nome ?: "Senza categoria",
                    colore      = cat?.colore ?: "#9E9E9E",
                    totale      = subtotal,
                    nSpese      = gruppo.size,
                    percentuale = if (totale > 0) (subtotal / totale).toFloat() else 0f,
                )
            }
            .sortedByDescending { it.totale }
        UiState.Successo(totale = totale, perCategoria = perCat)
    }
    .catch { e -> emit(UiState.Errore(e.message ?: "Errore sconosciuto")) }
    .stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Caricamento,
    )
}
