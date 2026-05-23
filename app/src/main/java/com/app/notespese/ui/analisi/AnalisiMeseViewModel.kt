package com.app.notespese.ui.analisi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.Spesa
import com.app.notespese.data.repository.BudgetRepository
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
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AnalisiMeseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    spesaRepository: SpesaRepository,
    categoriaRepository: CategoriaRepository,
    entrataRepository: EntrataRepository,
    gruppoRepository: GruppoRepository,
    budgetRepository: BudgetRepository,
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
        val percentualeBar: Float,
        val percentualeLabel: String,
        val budgetMensile: Double?,
        val superaBudget: Boolean,
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
            val spese: List<Spesa>,
            val membri: List<Membro>,
        ) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    val uiState: StateFlow<UiState> = combine(
        spesaRepository.osservaSpesePerMese(gruppoId, mese, anno),
        categoriaRepository.osservaCategorie(gruppoId),
        entrataRepository.osservaEntratePerMese(gruppoId, mese, anno),
        gruppoRepository.osservaMembri(gruppoId),
        budgetRepository.osservaBudget(gruppoId),
    ) { spese, categorie, entrate, membri, budgets ->
        val totaleSpese   = spese.sumOf { it.importo }
        val totaleEntrate = entrate.sumOf { it.importo }
        val fmt           = NumberFormat.getCurrencyInstance(Locale.ITALY)

        val perCategoria = spese
            .groupBy { it.categoriaId }
            .map { (catId, gruppo) ->
                val cat      = categorie.find { it.id == catId }
                val budget   = budgets.find { it.id == catId }
                val subtotal = gruppo.sumOf { it.importo }
                val budgetVal = budget?.importoMensile?.takeIf { it > 0 }

                val (bar, label, superaBudget) = if (budgetVal != null) {
                    val ratio = (subtotal / budgetVal).toFloat()
                    val label = "${fmt.format(subtotal)} / ${fmt.format(budgetVal)}"
                    Triple(ratio.coerceAtMost(1f), label, ratio > 1f)
                } else {
                    val base  = if (totaleEntrate > 0) totaleEntrate else totaleSpese
                    val ratio = if (base > 0) (subtotal / base).toFloat() else 0f
                    val ref   = if (totaleEntrate > 0) "entrate" else "tot."
                    Triple(ratio.coerceAtMost(1f), "${(ratio * 100).toInt()}% $ref", false)
                }

                CategoriaAnalisi(
                    categoriaId    = catId,
                    nome           = cat?.nome ?: "Senza categoria",
                    colore         = cat?.colore ?: "#9E9E9E",
                    totale         = subtotal,
                    nSpese         = gruppo.size,
                    percentualeBar = bar,
                    percentualeLabel = label,
                    budgetMensile  = budgetVal,
                    superaBudget   = superaBudget,
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
                PaganteAnalisi(userId, nome, subtotal, gruppo.size, perc, 0)
            }
            .sortedByDescending { it.totale }
            .mapIndexed { idx, p -> p.copy(coloreIdx = idx) }

        UiState.Successo(
            totaleSpese   = totaleSpese,
            totaleEntrate = totaleEntrate,
            perCategoria  = perCategoria,
            perPagante    = perPagante,
            spese         = spese,
            membri        = membri,
        ) as UiState
    }
    .catch { e -> emit(UiState.Errore(e.message ?: "Errore sconosciuto")) }
    .stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Caricamento,
    )
}
