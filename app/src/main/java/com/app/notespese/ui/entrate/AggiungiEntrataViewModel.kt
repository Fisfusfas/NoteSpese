package com.app.notespese.ui.entrate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Categoria
import com.app.notespese.data.model.Entrata
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.TipoCategoria
import com.app.notespese.data.repository.AuthRepository
import com.app.notespese.data.repository.CategoriaRepository
import com.app.notespese.data.repository.EntrataRepository
import com.app.notespese.data.repository.GruppoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AggiungiEntrataViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val entrataRepository: EntrataRepository,
    private val categoriaRepository: CategoriaRepository,
    gruppoRepository: GruppoRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val gruppoId: String = checkNotNull(savedStateHandle["gruppoId"])
    private val entrataId: String? = savedStateHandle.get<String>("entrataId")
    val isModifica: Boolean = entrataId != null

    // ── Form state ─────────────────────────────────────────────────────────────
    var importoText   by mutableStateOf("")
    var persona       by mutableStateOf("")
    var categoriaId   by mutableStateOf("")
    var mese          by mutableIntStateOf(YearMonth.now().monthValue)
    var anno          by mutableIntStateOf(YearMonth.now().year)
    var note          by mutableStateOf("")
    var erroreImporto by mutableStateOf(false)

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
        .map { list -> list.filter { it.tipo != TipoCategoria.SPESA.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val membri: StateFlow<List<Membro>> = gruppoRepository
        .osservaMembri(gruppoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val utente = authRepository.utenteCorrente.first()
            if (utente != null) persona = utente.id
        }
        entrataId?.let { id ->
            viewModelScope.launch {
                entrataRepository.getEntrata(gruppoId, id).onSuccess { entrata ->
                    entrata?.let {
                        importoText = it.importo.toBigDecimal().stripTrailingZeros().toPlainString()
                        persona     = it.persona
                        categoriaId = it.categoriaId
                        mese        = it.mese
                        anno        = it.anno
                        note        = it.note
                    }
                }
            }
        }
    }

    fun mesePrecedente() {
        val ym = YearMonth.of(anno, mese).minusMonths(1)
        mese = ym.monthValue; anno = ym.year
    }

    fun meseSuccessivo() {
        val ym = YearMonth.of(anno, mese).plusMonths(1)
        mese = ym.monthValue; anno = ym.year
    }

    fun salva() {
        val importoDouble = importoText.replace(",", ".").toDoubleOrNull()
        if (importoDouble == null || importoDouble <= 0.0) {
            erroreImporto = true
            return
        }
        erroreImporto = false
        val dataTs = com.google.firebase.Timestamp(
            Date.from(LocalDate.of(anno, mese, 1).atStartOfDay(ZoneId.systemDefault()).toInstant())
        )
        val entrata = Entrata(
            id          = entrataId ?: "",
            importo     = importoDouble,
            persona     = persona,
            categoriaId = categoriaId,
            mese        = mese,
            anno        = anno,
            note        = note.trim(),
            data        = dataTs,
        )
        viewModelScope.launch {
            esito = Esito.Caricamento
            val result: Result<*> = if (isModifica) {
                entrataRepository.aggiornaEntrata(gruppoId, entrata)
            } else {
                entrataRepository.aggiungiEntrata(gruppoId, entrata)
            }
            esito = result.fold(
                onSuccess = { Esito.Salvato },
                onFailure = { Esito.Errore(it.message ?: "Errore nel salvataggio") },
            )
        }
    }

    fun creaCategoria(nome: String, colore: String, icona: String = "label") {
        viewModelScope.launch {
            val cat = Categoria(nome = nome.trim(), icona = icona, colore = colore, tipo = TipoCategoria.ENTRATA.name)
            categoriaRepository.aggiungiCategoria(gruppoId, cat).onSuccess { id ->
                categoriaId = id
            }
        }
    }
}
