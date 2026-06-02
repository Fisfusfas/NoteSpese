package com.app.notespese.ui.entrate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.data.model.Categoria
import com.app.notespese.data.model.Entrata
import com.app.notespese.data.model.Membro
import com.app.notespese.ui.common.SelectoreMese
import com.app.notespese.ui.common.StatoVuoto
import com.app.notespese.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun EntrataScreen(
    onNavigateBack: () -> Unit,
    onAggiungiEntrata: (String) -> Unit,
    onModificaEntrata: (gruppoId: String, entrataId: String) -> Unit,
    viewModel: EntrataViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is EntrataViewModel.UiState.Caricamento -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is EntrataViewModel.UiState.Errore -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.messaggio, color = MaterialTheme.colorScheme.error)
            }
        }
        is EntrataViewModel.UiState.Successo -> {
            EntrataContent(
                state             = state,
                onNavigateBack    = onNavigateBack,
                onAggiungiEntrata = { onAggiungiEntrata(viewModel.gruppoId) },
                onEliminaEntrata  = viewModel::eliminaEntrata,
                onModificaEntrata = { entrataId -> onModificaEntrata(viewModel.gruppoId, entrataId) },
                onMesePrecedente  = viewModel::mesePrecedente,
                onMeseSuccessivo  = viewModel::meseSuccessivo,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntrataContent(
    state: EntrataViewModel.UiState.Successo,
    onNavigateBack: () -> Unit,
    onAggiungiEntrata: () -> Unit,
    onEliminaEntrata: (String) -> Unit,
    onModificaEntrata: (String) -> Unit,
    onMesePrecedente: () -> Unit,
    onMeseSuccessivo: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrate — ${state.nomeGruppo}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAggiungiEntrata) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi entrata")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item {
                SelectoreMese(
                    periodoLabel = state.periodoLabel,
                    onPrecedente = onMesePrecedente,
                    onSuccessivo = onMeseSuccessivo,
                )
            }
            if (state.entrate.isNotEmpty()) {
                item {
                    val totale = state.entrate.sumOf { it.importo }
                    Text(
                        text     = "Totale: ${NumberFormat.getCurrencyInstance(Locale.ITALY).format(totale)}  ·  ${state.entrate.size} operazioni",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    HorizontalDivider()
                }
            }
            if (state.entrate.isEmpty()) {
                item {
                    StatoVuoto("Nessuna entrata questo mese.\nPremi + per aggiungerne una.")
                }
            } else {
                items(state.entrate, key = { it.id }) { entrata ->
                    val categoria = remember(entrata.categoriaId, state.categorie) {
                        state.categorie.find { it.id == entrata.categoriaId }
                    }
                    val membro = remember(entrata.persona, state.membri) {
                        state.membri.find { it.userId == entrata.persona }
                    }
                    EntrataSwipeItem(
                        entrata   = entrata,
                        categoria = categoria,
                        membro    = membro,
                        onDelete  = { onEliminaEntrata(entrata.id) },
                        onModifica = { onModificaEntrata(entrata.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntrataSwipeItem(
    entrata: Entrata,
    categoria: Categoria?,
    membro: Membro?,
    onDelete: () -> Unit,
    onModifica: () -> Unit,
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title   = { Text("Elimina entrata") },
            text    = { Text("Sei sicuro di voler eliminare questa entrata? L'operazione non è reversibile.") },
            confirmButton = {
                TextButton(onClick = { showConfirmDialog = false; onDelete() }) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Annulla") }
            },
        )
    }

    val dismissState = rememberSwipeToDismissBoxState(
        // Ritorna false: lo swipe torna indietro e mostra il dialog di conferma
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { showConfirmDialog = true }
            false
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
        RigaEntrata(entrata = entrata, categoria = categoria, membro = membro, onClick = onModifica)
    }
}

@Composable
private fun RigaEntrata(
    entrata: Entrata,
    categoria: Categoria?,
    membro: Membro?,
    onClick: () -> Unit,
) {
    val nomeMembro = membro?.nominativoLocale?.ifBlank { null }
        ?: membro?.userId?.take(10)
        ?: entrata.persona.take(10)

    val dataFormattata = remember(entrata.data) {
        entrata.data?.toDate()?.let { date ->
            val ld = Instant.ofEpochMilli(date.time).atZone(ZoneId.systemDefault()).toLocalDate()
            DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN).format(ld)
        } ?: ""
    }

    ListItem(
        modifier          = Modifier.clickable(onClick = onClick),
        headlineContent   = {
            Text(categoria?.nome ?: "Entrata", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    if (dataFormattata.isNotEmpty()) {
                        Text(
                            text  = dataFormattata,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text  = "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(nomeMembro, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                if (entrata.note.isNotBlank()) {
                    Text(
                        text     = entrata.note,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        leadingContent    = {
            Icon(
                imageVector        = Icons.Default.TrendingUp,
                contentDescription = null,
                tint               = SuccessGreen,
            )
        },
        trailingContent   = {
            Text(
                text       = NumberFormat.getCurrencyInstance(Locale.ITALY).format(entrata.importo),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = SuccessGreen,
            )
        },
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

// ── Tab content (no Scaffold, for use inside GruppoHomeScreen) ────────────────

@Composable
fun EntrataListContent(
    onAggiungiEntrata: () -> Unit,
    onModificaEntrata: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EntrataViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is EntrataViewModel.UiState.Caricamento -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        is EntrataViewModel.UiState.Errore -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.messaggio, color = MaterialTheme.colorScheme.error)
            }
        }
        is EntrataViewModel.UiState.Successo -> {
            LazyColumn(
                modifier       = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                item {
                    SelectoreMese(
                        periodoLabel  = state.periodoLabel,
                        onPrecedente  = viewModel::mesePrecedente,
                        onSuccessivo  = viewModel::meseSuccessivo,
                        onTornaAdOggi = viewModel::tornaAdOggi,
                    )
                }
                if (state.entrate.isNotEmpty()) {
                    item {
                        val totale = state.entrate.sumOf { it.importo }
                        Text(
                            text     = "Totale: ${NumberFormat.getCurrencyInstance(Locale.ITALY).format(totale)}  ·  ${state.entrate.size} operazioni",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        HorizontalDivider()
                    }
                }
                if (state.entrate.isEmpty()) {
                    item {
                        StatoVuoto("Nessuna entrata questo periodo.\nPremi + per aggiungerne una.")
                    }
                } else {
                    items(state.entrate, key = { it.id }) { entrata ->
                        val categoria = remember(entrata.categoriaId, state.categorie) {
                            state.categorie.find { it.id == entrata.categoriaId }
                        }
                        val membro = remember(entrata.persona, state.membri) {
                            state.membri.find { it.userId == entrata.persona }
                        }
                        EntrataSwipeItem(
                            entrata    = entrata,
                            categoria  = categoria,
                            membro     = membro,
                            onDelete   = { viewModel.eliminaEntrata(entrata.id) },
                            onModifica = { onModificaEntrata(entrata.id) },
                        )
                    }
                }
            }
        }
    }
}
