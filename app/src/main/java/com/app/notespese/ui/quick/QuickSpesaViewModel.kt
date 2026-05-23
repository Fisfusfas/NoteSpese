package com.app.notespese.ui.quick

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.notespese.data.model.Categoria
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.Spesa
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class QuickSpesaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val spesaRepository: SpesaRepository,
    categoriaRepository: CategoriaRepository,
    gruppoRepository: GruppoRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val gruppoId: String = savedStateHandle["gruppoId"] ?: ""

    val categorie: StateFlow<List<Categoria>> = categoriaRepository.osservaCategorie(gruppoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val membri: StateFlow<List<Membro>> = gruppoRepository.osservaMembri(gruppoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var importoText   by mutableStateOf("")
    var descrizione   by mutableStateOf("")
    var categoriaId   by mutableStateOf("")
    var pagante       by mutableStateOf("")
    var condivisa     by mutableStateOf(true)
    var tipoFissa     by mutableStateOf(false)
    var erroreImporto by mutableStateOf(false)

    init {
        viewModelScope.launch {
            pagante = authRepository.utenteCorrente.first()?.id ?: ""
        }
    }

    sealed interface Esito {
        data object Idle : Esito
        data object Caricamento : Esito
        data object Salvato : Esito
        data class Errore(val msg: String) : Esito
    }
    var esito by mutableStateOf<Esito>(Esito.Idle)

    fun salva() {
        val importo = importoText.replace(',', '.').toDoubleOrNull()
        if (importo == null || importo <= 0) { erroreImporto = true; return }
        esito = Esito.Caricamento
        viewModelScope.launch {
            val oggi  = LocalDate.now()
            val ts    = Timestamp(oggi.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond, 0)
            val spesa = Spesa(
                importo     = importo,
                descrizione = descrizione.trim(),
                categoriaId = categoriaId,
                pagante     = pagante,
                condivisa   = condivisa,
                tipo        = if (tipoFissa) TipoSpesa.FISSA.name else TipoSpesa.VARIABILE.name,
                data        = ts,
                mese        = oggi.monthValue,
                anno        = oggi.year,
                note        = "",
            )
            esito = spesaRepository.aggiungiSpesa(gruppoId, spesa)
                .fold(onSuccess = { Esito.Salvato }, onFailure = { Esito.Errore(it.message ?: "Errore") })
        }
    }
}
