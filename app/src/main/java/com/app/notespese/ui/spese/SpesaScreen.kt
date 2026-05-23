package com.app.notespese.ui.spese

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.data.model.Spesa
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SpesaScreen(
    onNavigateBack: () -> Unit,
    onAggiungiSpesa: (String) -> Unit,
    viewModel: SpesaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is SpesaViewModel.UiState.Caricamento -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is SpesaViewModel.UiState.Errore -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.messaggio, color = MaterialTheme.colorScheme.error)
            }
        }
        is SpesaViewModel.UiState.Successo -> {
            SpesaContent(
                state          = state,
                onNavigateBack = onNavigateBack,
                onAggiungiSpesa = { onAggiungiSpesa(viewModel.gruppoId) },
                onEliminaSpesa = viewModel::eliminaSpesa,
                onMesePrecedente = viewModel::mesePrecedente,
                onMeseSuccessivo = viewModel::meseSuccessivo,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpesaContent(
    state: SpesaViewModel.UiState.Successo,
    onNavigateBack: () -> Unit,
    onAggiungiSpesa: () -> Unit,
    onEliminaSpesa: (String) -> Unit,
    onMesePrecedente: () -> Unit,
    onMeseSuccessivo: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spese — ${state.nomeGruppo}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAggiungiSpesa) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi spesa")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {

            // ── Selettore mese ────────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onMesePrecedente) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mese precedente")
                    }
                    val etichetta = remember(state.mese, state.anno) {
                        val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN)
                        java.time.LocalDate.of(state.anno, state.mese, 1)
                            .format(fmt).replaceFirstChar { it.uppercase() }
                    }
                    Text(etichetta, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onMeseSuccessivo) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mese successivo")
                    }
                }
            }

            // ── Riepilogo ─────────────────────────────────────────────────────
            if (state.spese.isNotEmpty()) {
                item {
                    val totale = state.spese.sumOf { it.importo }
                    Text(
                        text     = "Totale: ${NumberFormat.getCurrencyInstance(Locale.ITALY).format(totale)}  ·  ${state.spese.size} operazioni",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    HorizontalDivider()
                }
            }

            // ── Lista spese ───────────────────────────────────────────────────
            if (state.spese.isEmpty()) {
                item {
                    Text(
                        text      = "Nessuna spesa questo mese.\nPremi + per aggiungerne una.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            } else {
                items(state.spese, key = { it.id }) { spesa ->
                    SpesaSwipeItem(
                        spesa    = spesa,
                        onDelete = { onEliminaSpesa(spesa.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpesaSwipeItem(
    spesa: Spesa,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        },
        positionalThreshold = { it * 0.4f },
    )
    SwipeToDismissBox(
        state                    = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent        = {
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
        RigaSpesa(spesa = spesa)
    }
}

@Composable
private fun RigaSpesa(spesa: Spesa) {
    val dataFormattata = remember(spesa.data) {
        spesa.data?.toDate()?.let { date ->
            val ld = Instant.ofEpochMilli(date.time).atZone(ZoneId.systemDefault()).toLocalDate()
            DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN).format(ld)
        } ?: ""
    }
    ListItem(
        headlineContent   = {
            Text(spesa.descrizione.ifBlank { "Spesa" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (dataFormattata.isNotEmpty()) Text(dataFormattata)
                if (spesa.condivisa) {
                    SuggestionChip(
                        onClick = {},
                        label   = { Text("Condivisa", style = MaterialTheme.typography.labelSmall) },
                        colors  = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        modifier = Modifier.size(height = 20.dp, width = 76.dp),
                    )
                }
            }
        },
        leadingContent    = {
            Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent   = {
            Text(
                text       = NumberFormat.getCurrencyInstance(Locale.ITALY).format(spesa.importo),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary,
            )
        },
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
