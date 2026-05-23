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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.data.model.Categoria
import com.app.notespese.ui.gruppi.parseColore

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
                title          = { Text("Categorie") },
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
                if (state.categorie.isEmpty()) {
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
                        items(state.categorie, key = { it.id }) { cat ->
                            CategoriaSwipeItem(
                                cat       = cat,
                                onModifica = { viewModel.apriModifica(cat) },
                                onDelete  = { viewModel.elimina(cat.id) },
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
    cat: Categoria,
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
            headlineContent  = { Text(cat.nome) },
            leadingContent   = {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(parseColore(cat.colore))
                )
            },
            modifier         = Modifier.background(MaterialTheme.colorScheme.surface).clickable(onClick = onModifica),
        )
    }
}
