package com.app.notespese.ui.budget

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Budget
import com.app.notespese.data.model.Categoria
import com.app.notespese.data.repository.BudgetRepository
import com.app.notespese.data.repository.CategoriaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetRepository: BudgetRepository,
    categoriaRepository: CategoriaRepository,
) : ViewModel() {

    val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])

    data class RigaBudget(
        val categoria: Categoria,
        val importoMensile: Double,
    )

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(val righe: List<RigaBudget>) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    val uiState: StateFlow<UiState> = combine(
        categoriaRepository.osservaCategorie(gruppoId),
        budgetRepository.osservaBudget(gruppoId),
    ) { categorie, budgets ->
        val righe = categorie.map { cat ->
            val budget = budgets.find { it.id == cat.id }
            RigaBudget(cat, budget?.importoMensile ?: 0.0)
        }
        UiState.Successo(righe) as UiState
    }
    .catch { e -> emit(UiState.Errore(e.message ?: "Errore")) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Caricamento)

    // ── Dialog ─────────────────────────────────────────────────────────────────
    var showDialog      by mutableStateOf(false)
    var dialogCategoria by mutableStateOf<Categoria?>(null)
    var dialogImporto   by mutableStateOf("")
    var salvando        by mutableStateOf(false)
    var errore          by mutableStateOf<String?>(null)

    fun apriDialog(riga: RigaBudget) {
        dialogCategoria = riga.categoria
        dialogImporto   = if (riga.importoMensile > 0) riga.importoMensile.toString() else ""
        errore          = null
        showDialog      = true
    }

    fun chiudiDialog() { showDialog = false }

    fun salva() {
        val cat     = dialogCategoria ?: return
        val importo = dialogImporto.replace(',', '.').toDoubleOrNull()
        if (importo == null || importo < 0) { errore = "Inserisci un importo valido"; return }
        salvando = true
        errore   = null
        viewModelScope.launch {
            budgetRepository.impostaBudget(gruppoId, Budget(id = cat.id, importoMensile = importo))
                .onFailure { errore = it.message ?: "Errore" }
            salvando  = false
            if (errore == null) showDialog = false
        }
    }

    fun rimuovi(categoriaId: String) {
        viewModelScope.launch { budgetRepository.eliminaBudget(gruppoId, categoriaId) }
        showDialog = false
    }
}
