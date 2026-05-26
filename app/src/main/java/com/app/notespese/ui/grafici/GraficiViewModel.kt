package com.app.notespese.ui.grafici

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.repository.EntrataRepository
import com.app.notespese.data.repository.GruppoRepository
import com.app.notespese.data.repository.SpesaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

private val PALETTE_UTENTI = listOf(
    Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFE65100),
    Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFFAD1457),
)

@HiltViewModel
class GraficiViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val spesaRepository: SpesaRepository,
    private val entrataRepository: EntrataRepository,
    private val gruppoRepository: GruppoRepository,
) : ViewModel() {

    val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])

    data class UtenteEntrata(
        val nome: String,
        val totale: Double,
        val colore: Color,
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
            val perUtente: List<UtenteEntrata>,
            val mesiBar: List<MeseBar>,
            val meseLabel: String,
            val totaleEntrate: Double,
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
                val now    = YearMonth.now()
                val membri = gruppoRepository.osservaMembri(gruppoId).first()

                val entrateMese = entrataRepository.osservaEntratePerMese(gruppoId, now.monthValue, now.year).first()
                val totaleEntrate = entrateMese.sumOf { it.importo }

                val perUtente = if (totaleEntrate > 0) {
                    entrateMese
                        .groupBy { it.persona }
                        .map { (persona, lista) ->
                            val membro = membri.find { it.userId == persona }
                            val nome   = membro?.nominativoLocale?.ifBlank { null } ?: persona.take(10)
                            val tot    = lista.sumOf { it.importo }
                            UtenteEntrata(
                                nome        = nome,
                                totale      = tot,
                                colore      = Color.Unspecified,
                                percentuale = (tot / totaleEntrate).toFloat(),
                            )
                        }
                        .sortedByDescending { it.totale }
                        .mapIndexed { idx, u -> u.copy(colore = PALETTE_UTENTI[idx % PALETTE_UTENTI.size]) }
                } else emptyList()

                val months      = (5 downTo 0).map { now.minusMonths(it.toLong()) }
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

                val meseLabel = java.time.LocalDate.of(now.year, now.monthValue, 1)
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN))
                    .replaceFirstChar { it.uppercase() }

                _uiState.value = UiState.Successo(perUtente, mesiBar, meseLabel, totaleEntrate)
            } catch (e: Exception) {
                _uiState.value = UiState.Errore(e.message ?: "Errore nel caricamento")
            }
        }
    }
}
