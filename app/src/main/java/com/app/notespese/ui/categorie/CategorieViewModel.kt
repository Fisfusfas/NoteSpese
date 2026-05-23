package com.app.notespese.ui.categorie

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Categoria
import com.app.notespese.data.repository.CategoriaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategorieViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoriaRepository: CategoriaRepository,
) : ViewModel() {

    val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])

    sealed interface UiState {
        data object Caricamento : UiState
        data class Successo(val categorie: List<Categoria>) : UiState
        data class Errore(val messaggio: String) : UiState
    }

    val uiState: StateFlow<UiState> = categoriaRepository.osservaCategorie(gruppoId)
        .map<List<Categoria>, UiState> { UiState.Successo(it) }
        .catch { e -> emit(UiState.Errore(e.message ?: "Errore")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Caricamento)

    // ── Dialog add/edit ────────────────────────────────────────────────────────
    var showDialog     by mutableStateOf(false)
    var editCategoria  by mutableStateOf<Categoria?>(null)
    var dialogNome     by mutableStateOf("")
    var dialogColore   by mutableStateOf("#1565C0")
    var salvando       by mutableStateOf(false)
    var errore         by mutableStateOf<String?>(null)

    fun apriAggiungi() {
        editCategoria = null
        dialogNome    = ""
        dialogColore  = "#1565C0"
        errore        = null
        showDialog    = true
    }

    fun apriModifica(cat: Categoria) {
        editCategoria = cat
        dialogNome    = cat.nome
        dialogColore  = cat.colore
        errore        = null
        showDialog    = true
    }

    fun chiudiDialog() { showDialog = false }

    fun salva() {
        if (dialogNome.isBlank()) { errore = "Il nome è obbligatorio"; return }
        salvando = true
        errore   = null
        viewModelScope.launch {
            val cat = editCategoria
            val result = if (cat == null) {
                categoriaRepository.aggiungiCategoria(
                    gruppoId,
                    Categoria(nome = dialogNome.trim(), colore = dialogColore),
                )
            } else {
                categoriaRepository.aggiornaCategoria(
                    gruppoId,
                    cat.copy(nome = dialogNome.trim(), colore = dialogColore),
                ).map { cat.id }
            }
            result.onFailure { errore = it.message ?: "Errore nel salvataggio" }
            salvando  = false
            if (result.isSuccess) showDialog = false
        }
    }

    fun elimina(categoriaId: String) {
        viewModelScope.launch {
            categoriaRepository.eliminaCategoria(gruppoId, categoriaId)
        }
    }
}
