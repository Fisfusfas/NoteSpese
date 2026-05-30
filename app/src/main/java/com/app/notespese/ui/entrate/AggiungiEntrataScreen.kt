package com.app.notespese.ui.entrate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.ui.common.CategoriaSelector
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AggiungiEntrataScreen(
    onNavigateBack: () -> Unit,
    viewModel: AggiungiEntrataViewModel = hiltViewModel(),
) {
    val esito = viewModel.esito
    LaunchedEffect(esito) {
        if (esito is AggiungiEntrataViewModel.Esito.Salvato) onNavigateBack()
    }

    val categorie by viewModel.categorie.collectAsStateWithLifecycle()
    val membri    by viewModel.membri.collectAsStateWithLifecycle()

    var dropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker   by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text(if (viewModel.isModifica) "Modifica entrata" else "Nuova entrata") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Importo rapido ─────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 20, 50, 100).forEach { amount ->
                    FilterChip(
                        selected = viewModel.importoText == amount.toString(),
                        onClick  = {
                            viewModel.importoText  = amount.toString()
                            viewModel.erroreImporto = false
                        },
                        label    = { Text("€$amount") },
                    )
                }
            }

            // ── Importo ────────────────────────────────────────────────────────
            OutlinedTextField(
                value          = viewModel.importoText,
                onValueChange  = { viewModel.importoText = it; viewModel.erroreImporto = false },
                label          = { Text("Importo *") },
                prefix         = { Text("€") },
                singleLine     = true,
                isError        = viewModel.erroreImporto,
                supportingText = if (viewModel.erroreImporto) {{ Text("Inserisci un importo valido") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier       = Modifier.fillMaxWidth(),
            )

            // ── Categoria ──────────────────────────────────────────────────────
            CategoriaSelector(
                categorie              = categorie,
                categoriaSelezionataId = viewModel.categoriaId,
                onSeleziona            = { viewModel.categoriaId = it },
                onCreaCategoria        = { nome, colore, icona -> viewModel.creaCategoria(nome, colore, icona) },
            )

            // ── Chi ha ricevuto ────────────────────────────────────────────────
            if (membri.size > 1) {
                ExposedDropdownMenuBox(
                    expanded         = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                ) {
                    val membroCorrente = membri.find { it.userId == viewModel.persona }
                    val labelPersona = membroCorrente?.nominativoLocale?.ifBlank { null }
                        ?: membroCorrente?.userId?.take(10)
                        ?: "Seleziona"
                    OutlinedTextField(
                        value         = labelPersona,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Persona") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded         = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        membri.forEach { membro ->
                            val nome = membro.nominativoLocale.ifBlank { membro.userId.take(10) }
                            DropdownMenuItem(
                                text    = { Text(nome) },
                                onClick = { viewModel.persona = membro.userId; dropdownExpanded = false },
                            )
                        }
                    }
                }
            }

            // ── Data ───────────────────────────────────────────────────────────
            val dataLabel = remember(viewModel.dataSelezionata) {
                DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN).format(viewModel.dataSelezionata)
            }
            OutlinedTextField(
                value         = dataLabel,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Data") },
                trailingIcon  = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Scegli data")
                    }
                },
                modifier      = Modifier.fillMaxWidth(),
            )

            // ── Note ───────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = viewModel.note,
                onValueChange = { viewModel.note = it },
                label         = { Text("Note (opzionale)") },
                minLines      = 2,
                maxLines      = 4,
                modifier      = Modifier.fillMaxWidth(),
            )

            // ── Bottone salva ──────────────────────────────────────────────────
            Button(
                onClick  = { viewModel.salva() },
                enabled  = esito !is AggiungiEntrataViewModel.Esito.Caricamento,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (esito is AggiungiEntrataViewModel.Esito.Caricamento) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (viewModel.isModifica) "Aggiorna entrata" else "Salva entrata")
                }
            }

            if (esito is AggiungiEntrataViewModel.Esito.Errore) {
                Text(
                    text  = (esito as AggiungiEntrataViewModel.Esito.Errore).msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    // ── DatePickerDialog ───────────────────────────────────────────────────────
    if (showDatePicker) {
        val initialMillis = viewModel.dataSelezionata
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.dataSelezionata = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton    = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
