package com.app.notespese.ui.ricorrenze

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.data.model.Ricorrenza
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RicorrenzeScreen(
    onNavigateBack: () -> Unit,
    onAggiungi: () -> Unit,
    onModifica: (ricorrenzaId: String) -> Unit,
    viewModel: RicorrenzeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Ricorrenze") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAggiungi) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi ricorrenza")
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is RicorrenzeViewModel.UiState.Caricamento ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is RicorrenzeViewModel.UiState.Errore ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.messaggio, color = MaterialTheme.colorScheme.error)
                }
            is RicorrenzeViewModel.UiState.Successo -> {
                if (state.ricorrenze.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text      = "Nessuna ricorrenza.\nPremi + per aggiungerne una.",
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text      = "Le ricorrenze vengono create automaticamente\nall'inizio di ogni mese.",
                                style     = MaterialTheme.typography.bodySmall,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize().padding(innerPadding),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
                    ) {
                        items(state.ricorrenze, key = { it.id }) { ric ->
                            val categoria = remember(ric.categoriaId, state.categorie) {
                                state.categorie.find { it.id == ric.categoriaId }
                            }
                            val pagante = remember(ric.pagante, state.membri) {
                                state.membri.find { it.userId == ric.pagante }
                                    ?.nominativoLocale?.ifBlank { null } ?: ric.pagante.take(8).ifBlank { null }
                            }
                            RicorrenzaSwipeItem(
                                ric       = ric,
                                catNome   = categoria?.nome,
                                pagante   = pagante,
                                onModifica = { onModifica(ric.id) },
                                onDelete  = { viewModel.elimina(ric.id) },
                                onToggle  = { viewModel.toggleAttiva(ric) },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RicorrenzaSwipeItem(
    ric: Ricorrenza,
    catNome: String?,
    pagante: String?,
    onModifica: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
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
                Icon(Icons.Default.Delete, contentDescription = "Elimina",
                    tint     = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 20.dp).size(24.dp))
            }
        },
    ) {
        val fmt = NumberFormat.getCurrencyInstance(Locale.ITALY)
        ListItem(
            headlineContent   = {
                Text(
                    text       = ric.descrizione.ifBlank { catNome ?: "Ricorrenza" },
                    fontWeight = FontWeight.Medium,
                )
            },
            supportingContent = {
                val sub = buildString {
                    if (catNome != null) append(catNome)
                    append(" · giorno ${ric.giornoDelMese}")
                    if (pagante != null) append(" · $pagante")
                }
                Text(sub, style = MaterialTheme.typography.bodySmall)
            },
            trailingContent   = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(fmt.format(ric.importo), fontWeight = FontWeight.SemiBold)
                    Switch(checked = ric.attiva, onCheckedChange = { onToggle() })
                }
            },
            modifier          = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onModifica),
        )
    }
}
