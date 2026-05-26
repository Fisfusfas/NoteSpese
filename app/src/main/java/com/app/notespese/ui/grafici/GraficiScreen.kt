package com.app.notespese.ui.grafici

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraficiScreen(
    onNavigateBack: () -> Unit,
    viewModel: GraficiViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiche") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::aggiorna) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aggiorna")
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is GraficiViewModel.UiState.Caricamento ->
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is GraficiViewModel.UiState.Errore ->
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(state.messaggio, color = MaterialTheme.colorScheme.error)
                }
            is GraficiViewModel.UiState.Successo ->
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { CardEntratePerUtente(state) }
                    item { CardTrendMensile(state.mesiBar) }
                }
        }
    }
}

// ── Entrate per utente ─────────────────────────────────────────────────────────

@Composable
private fun CardEntratePerUtente(state: GraficiViewModel.UiState.Successo) {
    val fmt = NumberFormat.getCurrencyInstance(Locale.ITALY)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Entrate per persona", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text  = state.meseLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            if (state.perUtente.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Nessuna entrata questo mese",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.perUtente.forEach { utente ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text     = utente.nome,
                                    style    = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text       = fmt.format(utente.totale),
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = utente.colore,
                                )
                            }
                            // Barra orizzontale proporzionale
                            Row(
                                modifier          = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(utente.percentuale.coerceAtLeast(0.02f))
                                        .height(10.dp)
                                        .background(utente.colore, RoundedCornerShape(5.dp))
                                )
                                if (utente.percentuale < 1f) {
                                    Box(
                                        modifier = Modifier
                                            .weight((1f - utente.percentuale).coerceAtLeast(0.02f))
                                            .height(10.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                                RoundedCornerShape(5.dp),
                                            )
                                    )
                                }
                            }
                            Text(
                                text  = "${(utente.percentuale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Trend 6 mesi (barre) ───────────────────────────────────────────────────────

@Composable
private fun CardTrendMensile(mesi: List<GraficiViewModel.MeseBar>) {
    val rosso = MaterialTheme.colorScheme.error
    val verde = Color(0xFF4CAF50)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Trend ultimi 6 mesi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            GraficoBarreMensili(mesi = mesi, rosso = rosso, verde = verde)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendaItem(colore = rosso, label = "Spese")
                LegendaItem(colore = verde, label = "Entrate")
            }
        }
    }
}

@Composable
private fun GraficoBarreMensili(
    mesi: List<GraficiViewModel.MeseBar>,
    rosso: Color,
    verde: Color,
    modifier: Modifier = Modifier,
) {
    val altezzaMax = 120.dp
    val maxVal = mesi.maxOfOrNull { maxOf(it.spese, it.entrate) }?.coerceAtLeast(1.0) ?: 1.0

    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.Bottom,
    ) {
        mesi.forEach { mese ->
            Column(
                modifier            = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier              = Modifier.height(altezzaMax),
                    verticalAlignment     = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                ) {
                    val speseH   = (altezzaMax * (mese.spese / maxVal).toFloat()).coerceAtLeast(2.dp)
                    val entrateH = (altezzaMax * (mese.entrate / maxVal).toFloat()).coerceAtLeast(2.dp)
                    Box(
                        modifier = Modifier
                            .width(9.dp)
                            .height(speseH)
                            .background(rosso, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(9.dp)
                            .height(entrateH)
                            .background(verde, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text      = mese.label,
                    style     = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines  = 1,
                )
            }
        }
    }
}

@Composable
private fun LegendaItem(colore: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .background(colore, RoundedCornerShape(2.dp))
        )
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
