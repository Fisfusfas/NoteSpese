package com.app.notespese.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.ui.gruppi.parseColore
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fmt = NumberFormat.getCurrencyInstance(Locale.ITALY)

    // ── Dialog impostazione budget ─────────────────────────────────────────────
    if (viewModel.showDialog) {
        val cat = viewModel.dialogCategoria
        AlertDialog(
            onDismissRequest = viewModel::chiudiDialog,
            title   = { Text("Budget — ${cat?.nome ?: ""}") },
            text    = {
                Column {
                    OutlinedTextField(
                        value           = viewModel.dialogImporto,
                        onValueChange   = { viewModel.dialogImporto = it; viewModel.errore = null },
                        label           = { Text("Limite mensile (€)") },
                        prefix          = { Text("€") },
                        singleLine      = true,
                        isError         = viewModel.errore != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier        = Modifier.fillMaxWidth(),
                    )
                    if (viewModel.errore != null) {
                        Text(viewModel.errore!!, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::salva, enabled = !viewModel.salvando) {
                    if (viewModel.salvando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Salva")
                }
            },
            dismissButton = {
                Row {
                    if (cat != null) {
                        TextButton(onClick = { viewModel.rimuovi(cat.id) }) {
                            Text("Rimuovi", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = viewModel::chiudiDialog) { Text("Annulla") }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Budget per categoria") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is BudgetViewModel.UiState.Caricamento ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is BudgetViewModel.UiState.Errore ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.messaggio, color = MaterialTheme.colorScheme.error)
                }
            is BudgetViewModel.UiState.Successo -> {
                if (state.righe.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text(
                            text      = "Nessuna categoria trovata.\nCrea prima le categorie nelle impostazioni gruppo.",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(32.dp),
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        item {
                            Text(
                                text     = "Tocca una categoria per impostare o modificare il budget mensile.",
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(state.righe, key = { it.categoria.id }) { riga ->
                            ListItem(
                                headlineContent   = {
                                    Text(riga.categoria.nome, fontWeight = FontWeight.Medium)
                                },
                                supportingContent = {
                                    if (riga.importoMensile > 0)
                                        Text("Limite: ${fmt.format(riga.importoMensile)}")
                                    else
                                        Text("Nessun limite impostato",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                leadingContent    = {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(parseColore(riga.categoria.colore))
                                    )
                                },
                                modifier          = Modifier.clickable { viewModel.apriDialog(riga) },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}
