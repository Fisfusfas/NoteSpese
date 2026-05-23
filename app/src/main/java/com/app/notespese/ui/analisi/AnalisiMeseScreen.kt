package com.app.notespese.ui.analisi

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.ui.gruppi.parseColore
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalisiMeseScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalisiMeseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val meseLabel = remember(viewModel.mese, viewModel.anno) {
        val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN)
        java.time.LocalDate.of(viewModel.anno, viewModel.mese, 1)
            .format(fmt).replaceFirstChar { it.uppercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analisi — $meseLabel") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is AnalisiMeseViewModel.UiState.Caricamento -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AnalisiMeseViewModel.UiState.Errore -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.messaggio, color = MaterialTheme.colorScheme.error)
                }
            }
            is AnalisiMeseViewModel.UiState.Successo -> {
                if (state.perCategoria.isEmpty()) {
                    Box(
                        modifier         = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text      = "Nessuna spesa per questo mese.",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(32.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize().padding(innerPadding),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // ── Totale ─────────────────────────────────────────────────
                        item {
                            Card(
                                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier              = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically,
                                ) {
                                    Text("Totale del mese", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text       = NumberFormat.getCurrencyInstance(Locale.ITALY).format(state.totale),
                                        style      = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text  = "Per categoria",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // ── Per categoria ──────────────────────────────────────────
                        items(state.perCategoria) { cat ->
                            CardCategoria(cat = cat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardCategoria(cat: AnalisiMeseViewModel.CategoriaAnalisi) {
    val colore = parseColore(cat.colore)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(colore),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text     = cat.nome,
                    style    = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text       = NumberFormat.getCurrencyInstance(Locale.ITALY).format(cat.totale),
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress     = { cat.percentuale },
                modifier     = Modifier.fillMaxWidth(),
                color        = colore,
                trackColor   = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "${cat.nSpese} ${if (cat.nSpese == 1) "spesa" else "spese"}  ·  ${(cat.percentuale * 100).toInt()}% del totale",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
