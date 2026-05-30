package com.app.notespese.ui.entrate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.data.model.Categoria
import com.app.notespese.data.model.Entrata
import com.app.notespese.data.model.Membro
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                    Text(state.periodoLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onMeseSuccessivo) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mese successivo")
                    }
                }
            }

            // ── Riepilogo ─────────────────────────────────────────────────────
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

            // ── Lista entrate ─────────────────────────────────────────────────
            if (state.entrate.isEmpty()) {
                item {
                    Text(
                        text      = "Nessuna entrata questo mese.\nPremi + per aggiungerne una.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth().padding(32.dp),
                    )
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

    ListItem(
        modifier          = Modifier.clickable(onClick = onClick),
        headlineContent   = {
            Text(
                text     = categoria?.nome ?: "Entrata",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(nomeMembro, style = MaterialTheme.typography.bodySmall)
        },
        leadingContent    = {
            Icon(
                imageVector        = Icons.Default.TrendingUp,
                contentDescription = null,
                tint               = Color(0xFF2E7D32),
            )
        },
        trailingContent   = {
            Text(
                text       = NumberFormat.getCurrencyInstance(Locale.ITALY).format(entrata.importo),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF2E7D32),
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
            Box(modifier.fillMaxSize()) {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            IconButton(onClick = viewModel::mesePrecedente) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Periodo precedente")
                            }
                            Text(state.periodoLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = viewModel::meseSuccessivo) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Periodo successivo")
                            }
                        }
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
                            Text(
                                text      = "Nessuna entrata questo periodo.\nPremi + per aggiungerne una.",
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier.fillMaxWidth().padding(32.dp),
                            )
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
                FloatingActionButton(
                    onClick  = onAggiungiEntrata,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi entrata")
                }
            }
        }
    }
}
