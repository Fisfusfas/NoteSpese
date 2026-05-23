package com.app.notespese.ui.saldi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.ModalitaSplit
import com.app.notespese.data.model.Saldo
import com.app.notespese.data.model.StatoDebitore
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SaldoScreen(
    onNavigateBack: () -> Unit,
    viewModel: SaldoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Dialog is shown on top regardless of load state
    if (viewModel.showSplitDialog) {
        val state = uiState as? SaldoViewModel.UiState.Successo
        if (state != null) {
            SplitConfigDialog(
                modalita        = viewModel.splitModalita,
                onModalitaChange = { viewModel.splitModalita = it },
                pesi            = viewModel.splitPesi,
                onPesoChange    = { uid, v -> viewModel.splitPesi = viewModel.splitPesi + (uid to v) },
                membri          = state.membri,
                onDismiss       = viewModel::chiudiDialogSplit,
                onConferma      = viewModel::salvaSplit,
            )
        }
    }

    when (val state = uiState) {
        is SaldoViewModel.UiState.Caricamento -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        is SaldoViewModel.UiState.Errore -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.messaggio, color = MaterialTheme.colorScheme.error)
            }
        }
        is SaldoViewModel.UiState.Successo -> {
            LaunchedEffect(state.mese, state.anno) {
                viewModel.calcolaESalva()
            }
            SaldoContent(
                state               = state,
                azioneEsito         = viewModel.azioneEsito,
                onNavigateBack      = onNavigateBack,
                onCalcola           = viewModel::calcolaESalva,
                onApriSplitDialog   = viewModel::apriFialogSplit,
                onSegnaComePagato   = viewModel::segnaComePagato,
                onConfermaPagamento = viewModel::confermaPagamento,
                onMesePrecedente    = viewModel::mesePrecedente,
                onMeseSuccessivo    = viewModel::meseSuccessivo,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaldoContent(
    state: SaldoViewModel.UiState.Successo,
    azioneEsito: SaldoViewModel.AzioneEsito,
    onNavigateBack: () -> Unit,
    onCalcola: () -> Unit,
    onApriSplitDialog: () -> Unit,
    onSegnaComePagato: (String) -> Unit,
    onConfermaPagamento: (String) -> Unit,
    onMesePrecedente: () -> Unit,
    onMeseSuccessivo: () -> Unit,
) {
    val saldiAttivi  = state.saldi.filter { !it.isSaldato }
    val saldiChiusi  = state.saldi.filter {  it.isSaldato }
    val caricamento  = azioneEsito is SaldoViewModel.AzioneEsito.Caricamento

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saldi — ${state.nomeGruppo}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {

            // ── Selettore mese ────────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onMesePrecedente) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mese precedente")
                    }
                    val etichetta = remember(state.mese, state.anno) {
                        val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN)
                        LocalDate.of(state.anno, state.mese, 1).format(fmt).replaceFirstChar { it.uppercase() }
                    }
                    Text(etichetta, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onMeseSuccessivo) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mese successivo")
                    }
                }
            }

            // ── Bottone calcola + config suddivisione ─────────────────────────
            item {
                ElevatedButton(
                    onClick  = onCalcola,
                    enabled  = !caricamento,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    if (caricamento) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Ricalcola saldi del mese")
                }
                if (azioneEsito is SaldoViewModel.AzioneEsito.Errore) {
                    Text(
                        text     = (azioneEsito as SaldoViewModel.AzioneEsito.Errore).msg,
                        color    = MaterialTheme.colorScheme.error,
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                // Suddivisione row
                val splitLabel = when (state.meseConfig?.modalitaSplit) {
                    ModalitaSplit.CINQUANTA.name     -> "Equa (50/50)"
                    ModalitaSplit.COEFFICIENTE.name  -> "Coefficienti"
                    ModalitaSplit.PERSONALIZZATO.name -> "Percentuali fisse"
                    else                             -> "Equa (predefinita)"
                }
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text  = "Suddivisione: $splitLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onApriSplitDialog) { Text("Modifica") }
                }

                HorizontalDivider()
            }

            // ── Stato vuoto ───────────────────────────────────────────────────
            if (state.saldi.isEmpty()) {
                item {
                    Text(
                        text      = "Nessun saldo per questo mese.\nPremi \"Ricalcola\" dopo aver inserito le spese.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }

            // ── Saldi attivi ──────────────────────────────────────────────────
            if (saldiAttivi.isNotEmpty()) {
                item {
                    Text(
                        text     = "Da saldare",
                        style    = MaterialTheme.typography.titleSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(saldiAttivi, key = { it.id }) { saldo ->
                    CardSaldo(
                        saldo               = saldo,
                        membri              = state.membri,
                        userId              = state.userId,
                        caricamento         = caricamento,
                        onSegnaComePagato   = onSegnaComePagato,
                        onConfermaPagamento = onConfermaPagamento,
                    )
                }
            }

            // ── Saldi chiusi ──────────────────────────────────────────────────
            if (saldiChiusi.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text     = "Saldati",
                        style    = MaterialTheme.typography.titleSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(saldiChiusi, key = { "chiuso_${it.id}" }) { saldo ->
                    CardSaldo(
                        saldo               = saldo,
                        membri              = state.membri,
                        userId              = state.userId,
                        caricamento         = false,
                        onSegnaComePagato   = {},
                        onConfermaPagamento = {},
                    )
                }
            }
        }
    }
}

// ── Dialog configurazione suddivisione ────────────────────────────────────────

@Composable
private fun SplitConfigDialog(
    modalita: ModalitaSplit,
    onModalitaChange: (ModalitaSplit) -> Unit,
    pesi: Map<String, String>,
    onPesoChange: (String, String) -> Unit,
    membri: List<Membro>,
    onDismiss: () -> Unit,
    onConferma: () -> Unit,
) {
    val opzioni = listOf(
        Triple(ModalitaSplit.CINQUANTA,     "Equa (50/50)",         "Ogni spesa condivisa è divisa in parti uguali"),
        Triple(ModalitaSplit.COEFFICIENTE,  "Coefficienti",         "Le spese si dividono in base ai pesi (es. 1:2 per rapporto redditi)"),
        Triple(ModalitaSplit.PERSONALIZZATO,"Percentuali fisse",    "Inserisci la percentuale (es. 30 e 70, devono sommare 100)"),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Modalità di suddivisione") },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(modifier = Modifier.selectableGroup()) {
                    opzioni.forEach { (modo, etichetta, descrizione) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = modalita == modo,
                                    onClick  = { onModalitaChange(modo) },
                                    role     = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            RadioButton(selected = modalita == modo, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(etichetta, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(descrizione, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (modalita != ModalitaSplit.CINQUANTA) {
                    Spacer(Modifier.height(8.dp))
                    val hint = if (modalita == ModalitaSplit.PERSONALIZZATO) "Percentuale (%)" else "Peso (es. 1, 2)"
                    membri.forEach { membro ->
                        val nome = membro.nominativoLocale.ifBlank { membro.userId.take(8) }
                        OutlinedTextField(
                            value         = pesi[membro.userId] ?: "1",
                            onValueChange = { v -> onPesoChange(membro.userId, v) },
                            label         = { Text(nome) },
                            placeholder   = { Text(hint) },
                            singleLine    = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier      = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConferma) { Text("Salva e ricalcola") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}

// ── Card saldo ─────────────────────────────────────────────────────────────────

@Composable
private fun CardSaldo(
    saldo: Saldo,
    membri: List<Membro>,
    userId: String,
    caricamento: Boolean,
    onSegnaComePagato: (String) -> Unit,
    onConfermaPagamento: (String) -> Unit,
) {
    val nomeDebitore  = nomeMembro(saldo.da, membri)
    val nomeCreditore = nomeMembro(saldo.a,  membri)

    val sonoDebitore  = saldo.da == userId
    val sonoCreditore = saldo.a  == userId

    val containerColor = when {
        saldo.isSaldato                           -> MaterialTheme.colorScheme.surfaceVariant
        sonoDebitore                              -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        sonoCreditore                             -> Color(0xFF2E7D32).copy(alpha = 0.12f)
        else                                      -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors   = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(nomeDebitore, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Icon(Icons.Default.ArrowForward, contentDescription = null,
                        modifier = Modifier.padding(horizontal = 6.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(nomeCreditore, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = NumberFormat.getCurrencyInstance(Locale.ITALY).format(saldo.importoCalcolato),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = if (sonoDebitore) MaterialTheme.colorScheme.error
                                 else if (sonoCreditore) Color(0xFF2E7D32)
                                 else MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(4.dp))
            val statoTesto = when {
                saldo.isSaldato                                          -> "Saldato"
                saldo.statoDebitore == StatoDebitore.PAGATO.name        -> "Pagamento dichiarato — in attesa di conferma"
                else                                                     -> if (sonoDebitore) "Devi pagare" else "In attesa di pagamento"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (saldo.isSaldato) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(statoTesto, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (!saldo.isSaldato) {
                Spacer(Modifier.height(8.dp))
                when {
                    sonoDebitore && saldo.statoDebitore != StatoDebitore.PAGATO.name -> {
                        Button(
                            onClick  = { onSegnaComePagato(saldo.id) },
                            enabled  = !caricamento,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Segna come pagato") }
                    }
                    sonoCreditore && saldo.statoDebitore == StatoDebitore.PAGATO.name -> {
                        OutlinedButton(
                            onClick  = { onConfermaPagamento(saldo.id) },
                            enabled  = !caricamento,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Conferma ricezione") }
                    }
                }
            }
        }
    }
}

// ── Helper ─────────────────────────────────────────────────────────────────────

private fun nomeMembro(userId: String, membri: List<Membro>): String {
    val membro = membri.find { it.userId == userId }
    return membro?.nominativoLocale?.ifBlank { null } ?: userId.take(8)
}
