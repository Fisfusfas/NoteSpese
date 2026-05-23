package com.app.notespese.ui.analisi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.repository.CategoriaRepository
import com.app.notespese.data.repository.EntrataRepository
import com.app.notespese.data.repository.GruppoRepository
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
    entrataRepository: EntrataRepository,
    gruppoRepository: GruppoRepository,
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
        val percentualeBar: Float,    // 0..1 capped for the bar
        val percentualeLabel: String, // e.g. "45% entrate" or "45% tot."
    )

    data class PaganteAnalisi(
        val userId: String,
        val nome: String,
        val totale: Double,
        val nSpese: Int,
        val percentualeBar: Float,
        val coloreIdx: Int,
    )

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(
            val totaleSpese: Double,
            val totaleEntrate: Double,
            val perCategoria: List<CategoriaAnalisi>,
            val perPagante: List<PaganteAnalisi>,
        ) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    val uiState: StateFlow<UiState> = combine(
        spesaRepository.osservaSpesePerMese(gruppoId, mese, anno),
        categoriaRepository.osservaCategorie(gruppoId),
        entrataRepository.osservaEntratePerMese(gruppoId, mese, anno),
        gruppoRepository.osservaMembri(gruppoId),
    ) { spese, categorie, entrate, membri ->
        val totaleSpese   = spese.sumOf { it.importo }
        val totaleEntrate = entrate.sumOf { it.importo }
        val basePerc      = if (totaleEntrate > 0) totaleEntrate else totaleSpese
        val vsEntrate     = totaleEntrate > 0

        val perCategoria = spese
            .groupBy { it.categoriaId }
            .map { (catId, gruppo) ->
                val cat      = categorie.find { it.id == catId }
                val subtotal = gruppo.sumOf { it.importo }
                val perc     = if (basePerc > 0) (subtotal / basePerc).toFloat() else 0f
                CategoriaAnalisi(
                    categoriaId      = catId,
                    nome             = cat?.nome ?: "Senza categoria",
                    colore           = cat?.colore ?: "#9E9E9E",
                    totale           = subtotal,
                    nSpese           = gruppo.size,
                    percentualeBar   = perc.coerceAtMost(1f),
                    percentualeLabel = "${(perc * 100).toInt()}% ${if (vsEntrate) "entrate" else "tot."}",
                )
            }
            .sortedByDescending { it.totale }

        val perPagante = spese
            .filter { it.pagante.isNotBlank() }
            .groupBy { it.pagante }
            .map { (userId, gruppo) ->
                val membro   = membri.find { it.userId == userId }
                val nome     = membro?.nominativoLocale?.ifBlank { null } ?: userId.take(8)
                val subtotal = gruppo.sumOf { it.importo }
                val perc     = if (totaleSpese > 0) (subtotal / totaleSpese).toFloat() else 0f
                PaganteAnalisi(
                    userId         = userId,
                    nome           = nome,
                    totale         = subtotal,
                    nSpese         = gruppo.size,
                    percentualeBar = perc,
                    coloreIdx      = 0,
                )
            }
            .sortedByDescending { it.totale }
            .mapIndexed { idx, p -> p.copy(coloreIdx = idx) }

        UiState.Successo(
            totaleSpese   = totaleSpese,
            totaleEntrate = totaleEntrate,
            perCategoria  = perCategoria,
            perPagante    = perPagante,
        ) as UiState
    }
    .catch { e -> emit(UiState.Errore(e.message ?: "Errore sconosciuto")) }
    .stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Caricamento,
    )
}
