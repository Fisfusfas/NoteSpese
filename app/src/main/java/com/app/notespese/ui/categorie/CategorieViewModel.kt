package com.app.notespese.ui.categorie

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Budget
import com.app.notespese.data.model.Categoria
import com.app.notespese.data.model.TipoCategoria
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
class CategorieViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoriaRepository: CategoriaRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])

    data class RigaCategoria(
        val categoria: Categoria,
        val budgetMensile: Double,
    )

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(val righe: List<RigaCategoria>) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    val uiState: StateFlow<UiState> = combine(
        categoriaRepository.osservaCategorie(gruppoId),
        budgetRepository.osservaBudget(gruppoId),
    ) { categorie, budgets ->
        val righe = categorie.map { cat ->
            RigaCategoria(cat, budgets.find { it.id == cat.id }?.importoMensile ?: 0.0)
        }
        UiState.Successo(righe) as UiState
    }
    .catch { e -> emit(UiState.Errore(e.message ?: "Errore")) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Caricamento)

    // ── Dialog add/edit categoria ──────────────────────────────────────────────
    var showDialog         by mutableStateOf(false)
    var editCategoria      by mutableStateOf<Categoria?>(null)
    var dialogNome         by mutableStateOf("")
    var dialogColore       by mutableStateOf("#1565C0")
    var dialogIcona        by mutableStateOf("label")
    var dialogBudget       by mutableStateOf("")
    var dialogTipo         by mutableStateOf(TipoCategoria.SPESA.name)
    var salvando           by mutableStateOf(false)
    var errore             by mutableStateOf<String?>(null)

    fun apriAggiungi(defaultTipo: String = TipoCategoria.SPESA.name) {
        editCategoria = null
        dialogNome    = ""
        dialogColore  = "#1565C0"
        dialogIcona   = "label"
        dialogBudget  = ""
        dialogTipo    = defaultTipo
        errore        = null
        showDialog    = true
    }

    fun apriModifica(riga: RigaCategoria) {
        editCategoria = riga.categoria
        dialogNome    = riga.categoria.nome
        dialogColore  = riga.categoria.colore
        dialogIcona   = riga.categoria.icona
        dialogBudget  = if (riga.budgetMensile > 0) riga.budgetMensile.toString() else ""
        dialogTipo    = riga.categoria.tipo
        errore        = null
        showDialog    = true
    }

    fun chiudiDialog() { showDialog = false }

    fun salva() {
        if (dialogNome.isBlank()) { errore = "Il nome è obbligatorio"; return }
        val budgetImporto = dialogBudget.replace(',', '.').toDoubleOrNull()
            .let { if (dialogBudget.isBlank()) 0.0 else (it ?: run { errore = "Budget non valido"; return }) }
        if (budgetImporto < 0) { errore = "Il budget non può essere negativo"; return }

        salvando = true
        errore   = null
        viewModelScope.launch {
            val cat = editCategoria
            val result = if (cat == null) {
                categoriaRepository.aggiungiCategoria(
                    gruppoId,
                    Categoria(nome = dialogNome.trim(), colore = dialogColore, icona = dialogIcona, tipo = dialogTipo),
                ).also { r ->
                    r.getOrNull()?.let { newId ->
                        if (budgetImporto > 0)
                            budgetRepository.impostaBudget(gruppoId, Budget(id = newId, importoMensile = budgetImporto))
                    }
                }
            } else {
                categoriaRepository.aggiornaCategoria(
                    gruppoId,
                    cat.copy(nome = dialogNome.trim(), colore = dialogColore, icona = dialogIcona, tipo = dialogTipo),
                ).also {
                    if (budgetImporto > 0) {
                        budgetRepository.impostaBudget(gruppoId, Budget(id = cat.id, importoMensile = budgetImporto))
                    } else {
                        budgetRepository.eliminaBudget(gruppoId, cat.id)
                    }
                }.map { cat.id }
            }
            result.onFailure { errore = it.message ?: "Errore nel salvataggio" }
            salvando  = false
            if (result.isSuccess) showDialog = false
        }
    }

    fun elimina(categoriaId: String) {
        viewModelScope.launch {
            categoriaRepository.eliminaCategoria(gruppoId, categoriaId)
            budgetRepository.eliminaBudget(gruppoId, categoriaId)
        }
    }
}
