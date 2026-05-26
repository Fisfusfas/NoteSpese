package com.app.notespese.ui.grafici

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.repository.CategoriaRepository
import com.app.notespese.data.repository.EntrataRepository
import com.app.notespese.data.repository.SpesaRepository
import com.app.notespese.ui.gruppi.parseColore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GraficiViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val spesaRepository: SpesaRepository,
    private val entrataRepository: EntrataRepository,
    private val categoriaRepository: CategoriaRepository,
) : ViewModel() {

    val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])

    data class FettaTorta(
        val nome: String,
        val colore: Color,
        val totale: Double,
        val percentuale: Float,
    )

    data class MeseBar(
        val label: String,
        val spese: Double,
        val entrate: Double,
    )

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(
            val fette: List<FettaTorta>,
            val mesiBar: List<MeseBar>,
            val meseLabel: String,
            val totaleSpese: Double,
        ) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Caricamento)
    val uiState: StateFlow<UiState> = _uiState

    init { caricaDati() }

    fun aggiorna() = caricaDati()

    private fun caricaDati() {
        _uiState.value = UiState.Caricamento
        viewModelScope.launch {
            try {
                val now = YearMonth.now()
                val categorie = categoriaRepository.osservaCategorie(gruppoId).first()
                val speseMese = spesaRepository.osservaSpesePerMese(gruppoId, now.monthValue, now.year).first()

                val totale = speseMese.sumOf { it.importo }
                val fette = if (totale > 0) {
                    speseMese.groupBy { it.categoriaId }
                        .map { (catId, spese) ->
                            val cat = categorie.find { it.id == catId }
                            FettaTorta(
                                nome        = cat?.nome ?: "Senza categoria",
                                colore      = parseColore(cat?.colore ?: "#9E9E9E"),
                                totale      = spese.sumOf { it.importo },
                                percentuale = (spese.sumOf { it.importo } / totale).toFloat(),
                            )
                        }.sortedByDescending { it.totale }
                } else emptyList()

                val months = (5 downTo 0).map { now.minusMonths(it.toLong()) }
                val speseJobs   = months.map { ym -> async { spesaRepository.osservaSpesePerMese(gruppoId, ym.monthValue, ym.year).first() } }
                val entrateJobs = months.map { ym -> async { entrataRepository.osservaEntratePerMese(gruppoId, ym.monthValue, ym.year).first() } }
                val speseList   = speseJobs.map { it.await() }
                val entrateList = entrateJobs.map { it.await() }

                val mesiBar = months.mapIndexed { i, ym ->
                    MeseBar(
                        label   = ym.month.getDisplayName(TextStyle.SHORT, Locale.ITALIAN)
                            .replaceFirstChar { it.uppercase() },
                        spese   = speseList[i].sumOf { it.importo },
                        entrate = entrateList[i].sumOf { it.importo },
                    )
                }

                val meseLabel = LocalDate.of(now.year, now.monthValue, 1)
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN))
                    .replaceFirstChar { it.uppercase() }

                _uiState.value = UiState.Successo(fette, mesiBar, meseLabel, totale)
            } catch (e: Exception) {
                _uiState.value = UiState.Errore(e.message ?: "Errore nel caricamento")
            }
        }
    }
}
