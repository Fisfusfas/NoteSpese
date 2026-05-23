package com.app.notespese.ui.ricorrenze

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Categoria
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.Ricorrenza
import com.app.notespese.data.model.TipoSpesa
import com.app.notespese.data.repository.CategoriaRepository
import com.app.notespese.data.repository.GruppoRepository
import com.app.notespese.data.repository.RicorrenzaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AggiungiRicorrenzaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ricorrenzaRepository: RicorrenzaRepository,
    categoriaRepository: CategoriaRepository,
    gruppoRepository: GruppoRepository,
) : ViewModel() {

    private val gruppoId: String    = checkNotNull(savedStateHandle["gruppoId"])
    private val ricorrenzaId: String? = savedStateHandle["ricorrenzaId"]
    val isModifica: Boolean          = ricorrenzaId != null

    val categorie: StateFlow<List<Categoria>> = categoriaRepository.osservaCategorie(gruppoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val membri: StateFlow<List<Membro>> = gruppoRepository.osservaMembri(gruppoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var importoText     by mutableStateOf("")
    var descrizione     by mutableStateOf("")
    var categoriaId     by mutableStateOf("")
    var pagante         by mutableStateOf("")
    var condivisa       by mutableStateOf(true)
    var giornoDelMese   by mutableStateOf("1")
    var tipo            by mutableStateOf(TipoSpesa.FISSA.name)
    var erroreImporto   by mutableStateOf(false)

    sealed interface Esito { data object Idle : Esito; data object Caricamento : Esito
        data object Salvato : Esito; data class Errore(val msg: String) : Esito }
    var esito by mutableStateOf<Esito>(Esito.Idle)

    init {
        if (ricorrenzaId != null) {
            viewModelScope.launch {
                ricorrenzaRepository.getRicorrenza(gruppoId, ricorrenzaId).onSuccess { ric ->
                    if (ric != null) {
                        importoText   = ric.importo.toString()
                        descrizione   = ric.descrizione
                        categoriaId   = ric.categoriaId
                        pagante       = ric.pagante
                        condivisa     = ric.condivisa
                        giornoDelMese = ric.giornoDelMese.toString()
                        tipo          = ric.tipo
                    }
                }
            }
        }
    }

    fun salva() {
        val importo = importoText.replace(',', '.').toDoubleOrNull()
        if (importo == null || importo <= 0) { erroreImporto = true; return }
        val giorno = giornoDelMese.toIntOrNull()?.coerceIn(1, 31) ?: 1
        esito = Esito.Caricamento
        viewModelScope.launch {
            val ric = Ricorrenza(
                id            = ricorrenzaId ?: "",
                importo       = importo,
                descrizione   = descrizione.trim(),
                categoriaId   = categoriaId,
                pagante       = pagante,
                condivisa     = condivisa,
                giornoDelMese = giorno,
                tipo          = tipo,
                attiva        = true,
            )
            val result = if (isModifica) ricorrenzaRepository.aggiornaRicorrenza(gruppoId, ric)
                         else            ricorrenzaRepository.aggiungiRicorrenza(gruppoId, ric).map { }
            esito = result.fold(onSuccess = { Esito.Salvato }, onFailure = { Esito.Errore(it.message ?: "Errore") })
        }
    }
}
