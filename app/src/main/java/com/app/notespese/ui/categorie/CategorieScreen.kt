package com.app.notespese.ui.categorie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.ui.gruppi.parseColore
import java.text.NumberFormat
import java.util.Locale

private val PALETTE_COLORI = listOf(
    "#1565C0", "#2E7D32", "#E65100", "#6A1B9A",
    "#00838F", "#AD1457", "#F9A825", "#37474F",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorieScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategorieViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fmt = NumberFormat.getCurrencyInstance(Locale.ITALY)

    // ── Dialog add/edit ────────────────────────────────────────────────────────
    if (viewModel.showDialog) {
        AlertDialog(
            onDismissRequest = viewModel::chiudiDialog,
            title   = { Text(if (viewModel.editCategoria == null) "Nuova categoria" else "Modifica categoria") },
            text    = {
                Column {
                    OutlinedTextField(
                        value         = viewModel.dialogNome,
                        onValueChange = { viewModel.dialogNome = it; viewModel.errore = null },
                        label         = { Text("Nome *") },
                        singleLine    = true,
                        isError       = viewModel.errore != null,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                    if (viewModel.errore != null) {
                        Text(viewModel.errore!!, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value           = viewModel.dialogBudget,
                        onValueChange   = { viewModel.dialogBudget = it; viewModel.errore = null },
                        label           = { Text("Budget mensile (€, opzionale)") },
                        prefix          = { Text("€") },
                        singleLine      = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier        = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Colore", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        items(PALETTE_COLORI) { hex ->
                            val colore = parseColore(hex)
                            val selected = hex == viewModel.dialogColore
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 36.dp else 32.dp)
                                    .clip(CircleShape)
                                    .background(colore)
                                    .clickable { viewModel.dialogColore = hex },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Box(Modifier.size(14.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::salva, enabled = !viewModel.salvando) {
                    if (viewModel.salvando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Salva")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::chiudiDialog) { Text("Annulla") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Categorie e budget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::apriAggiungi) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi categoria")
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is CategorieViewModel.UiState.Caricamento ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is CategorieViewModel.UiState.Errore ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.messaggio, color = MaterialTheme.colorScheme.error)
                }
            is CategorieViewModel.UiState.Successo -> {
                if (state.righe.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text(
                            text      = "Nessuna categoria.\nPremi + per aggiungerne una.",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize().padding(innerPadding),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
                    ) {
                        items(state.righe, key = { it.categoria.id }) { riga ->
                            CategoriaSwipeItem(
                                riga      = riga,
                                fmt       = fmt,
                                onModifica = { viewModel.apriModifica(riga) },
                                onDelete  = { viewModel.elimina(riga.categoria.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriaSwipeItem(
    riga: CategorieViewModel.RigaCategoria,
    fmt: java.text.NumberFormat,
    onModifica: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        },
        positionalThreshold = { it * 0.4f },
    )
    SwipeToDismissBox(
        state                       = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent           = {
            Box(
                modifier         = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Elimina",
                    tint               = MaterialTheme.colorScheme.onErrorContainer,
                    modifier           = Modifier.padding(end = 20.dp).size(24.dp),
                )
            }
        },
    ) {
        ListItem(
            headlineContent  = { Text(riga.categoria.nome) },
            supportingContent = {
                if (riga.budgetMensile > 0)
                    Text("Budget: ${fmt.format(riga.budgetMensile)}", style = MaterialTheme.typography.bodySmall)
                else
                    Text("Nessun budget", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            leadingContent   = {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(parseColore(riga.categoria.colore))
                )
            },
            modifier         = Modifier.background(MaterialTheme.colorScheme.surface).clickable(onClick = onModifica),
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}
