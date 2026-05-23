package com.app.notespese.ui.analisi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Entrata
import com.app.notespese.data.model.Membro
import com.app.notespese.data.repository.CategoriaRepository
import com.app.notespese.data.repository.EntrataRepository
import com.app.notespese.data.repository.GruppoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AnalisiEntrateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    entrataRepository: EntrataRepository,
    categoriaRepository: CategoriaRepository,
    gruppoRepository: GruppoRepository,
) : ViewModel() {

    val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])
    val mese: Int        = checkNotNull(savedStateHandle["mese"])
    val anno: Int        = checkNotNull(savedStateHandle["anno"])

    data class CategoriaEntrataAnalisi(
        val categoriaId: String,
        val nome: String,
        val colore: String,
        val totale: Double,
        val nEntrate: Int,
        val percentualeBar: Float,
        val percentualeLabel: String,
    )

    data class PersonaAnalisi(
        val userId: String,
        val nome: String,
        val totale: Double,
        val nEntrate: Int,
        val percentualeBar: Float,
        val coloreIdx: Int,
    )

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(
            val totale: Double,
            val perCategoria: List<CategoriaEntrataAnalisi>,
            val perPersona: List<PersonaAnalisi>,
            val entrate: List<Entrata>,
            val membri: List<Membro>,
        ) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    val uiState: StateFlow<UiState> = combine(
        entrataRepository.osservaEntratePerMese(gruppoId, mese, anno),
        categoriaRepository.osservaCategorie(gruppoId),
        gruppoRepository.osservaMembri(gruppoId),
    ) { entrate, categorie, membri ->
        val totale = entrate.sumOf { it.importo }

        val perCategoria = entrate
            .groupBy { it.categoriaId }
            .map { (catId, gruppo) ->
                val cat      = categorie.find { it.id == catId }
                val subtotal = gruppo.sumOf { it.importo }
                val perc     = if (totale > 0) (subtotal / totale).toFloat() else 0f
                CategoriaEntrataAnalisi(
                    categoriaId      = catId,
                    nome             = cat?.nome ?: "Senza categoria",
                    colore           = cat?.colore ?: "#9E9E9E",
                    totale           = subtotal,
                    nEntrate         = gruppo.size,
                    percentualeBar   = perc.coerceAtMost(1f),
                    percentualeLabel = "${(perc * 100).toInt()}% tot.",
                )
            }
            .sortedByDescending { it.totale }

        val perPersona = entrate
            .filter { it.persona.isNotBlank() }
            .groupBy { it.persona }
            .map { (userId, gruppo) ->
                val membro   = membri.find { it.userId == userId }
                val nome     = membro?.nominativoLocale?.ifBlank { null } ?: userId.take(8)
                val subtotal = gruppo.sumOf { it.importo }
                val perc     = if (totale > 0) (subtotal / totale).toFloat() else 0f
                PersonaAnalisi(userId, nome, subtotal, gruppo.size, perc, 0)
            }
            .sortedByDescending { it.totale }
            .mapIndexed { idx, p -> p.copy(coloreIdx = idx) }

        UiState.Successo(
            totale       = totale,
            perCategoria = perCategoria,
            perPersona   = perPersona,
            entrate      = entrate,
            membri       = membri,
        ) as UiState
    }
    .catch { e -> emit(UiState.Errore(e.message ?: "Errore sconosciuto")) }
    .stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Caricamento,
    )
}
