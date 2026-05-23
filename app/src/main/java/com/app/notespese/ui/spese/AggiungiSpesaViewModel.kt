package com.app.notespese.ui.spese

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Categoria
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.Spesa
import com.app.notespese.data.model.TipoCategoria
import com.app.notespese.data.model.TipoSpesa
import com.app.notespese.data.repository.AuthRepository
import com.app.notespese.data.repository.CategoriaRepository
import com.app.notespese.data.repository.GruppoRepository
import com.app.notespese.data.repository.SpesaRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AggiungiSpesaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val spesaRepository: SpesaRepository,
    private val categoriaRepository: CategoriaRepository,
    gruppoRepository: GruppoRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])

    // ── Form state ─────────────────────────────────────────────────────────────
    var importoText     by mutableStateOf("")
    var descrizione     by mutableStateOf("")
    var categoriaId     by mutableStateOf("")
    var pagante         by mutableStateOf("")
    var condivisa       by mutableStateOf(true)
    var tipo            by mutableStateOf(TipoSpesa.VARIABILE)
    var dataSelezionata by mutableStateOf(LocalDate.now())
    var note            by mutableStateOf("")
    var erroreImporto   by mutableStateOf(false)

    sealed interface Esito {
        data object Inattivo    : Esito
        data object Caricamento : Esito
        data object Salvato     : Esito
        data class  Errore(val msg: String) : Esito
    }
    var esito by mutableStateOf<Esito>(Esito.Inattivo)

    // ── Dati caricati ──────────────────────────────────────────────────────────
    val categorie: StateFlow<List<Categoria>> = categoriaRepository
        .osservaCategorie(gruppoId)
        .map { list -> list.filter { it.tipo != TipoCategoria.ENTRATA.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val membri: StateFlow<List<Membro>> = gruppoRepository
        .osservaMembri(gruppoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val utente = authRepository.utenteCorrente.first()
            if (utente != null) pagante = utente.id
        }
    }

    // ── Azioni ────────────────────────────────────────────────────────────────
    fun salva() {
        val importoDouble = importoText.replace(",", ".").toDoubleOrNull()
        if (importoDouble == null || importoDouble <= 0.0) {
            erroreImporto = true
            return
        }
        erroreImporto = false
        val dataFirebase = Timestamp(Date.from(dataSelezionata.atStartOfDay(ZoneId.systemDefault()).toInstant()))
        val spesa = Spesa(
            importo     = importoDouble,
            descrizione = descrizione.trim(),
            categoriaId = categoriaId,
            pagante     = pagante,
            condivisa   = condivisa,
            tipo        = tipo.name,
            data        = dataFirebase,
            mese        = dataSelezionata.monthValue,
            anno        = dataSelezionata.year,
            note        = note.trim(),
        )
        viewModelScope.launch {
            esito = Esito.Caricamento
            esito = spesaRepository.aggiungiSpesa(gruppoId, spesa).fold(
                onSuccess = { Esito.Salvato },
                onFailure = { Esito.Errore(it.message ?: "Errore nel salvataggio") },
            )
        }
    }

    fun creaCategoria(nome: String, colore: String) {
        viewModelScope.launch {
            val cat = Categoria(nome = nome.trim(), icona = "label", colore = colore, tipo = TipoCategoria.SPESA.name)
            categoriaRepository.aggiungiCategoria(gruppoId, cat).onSuccess { id ->
                categoriaId = id
            }
        }
    }
}
