package com.app.notespese.ui.analisi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.Spesa
import com.app.notespese.ui.gruppi.parseColore
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern
import java.util.Locale

private val PALETTE_PAGANTI = listOf(
    Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFE65100),
    Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFFAD1457),
)
private fun colorePagante(idx: Int): Color = PALETTE_PAGANTI[idx % PALETTE_PAGANTI.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalisiMeseScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalisiMeseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val meseLabel = remember(viewModel.mese, viewModel.anno) {
        val fmt = ofPattern("MMMM yyyy", Locale.ITALIAN)
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
            is AnalisiMeseViewModel.UiState.Caricamento ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is AnalisiMeseViewModel.UiState.Errore ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.messaggio, color = MaterialTheme.colorScheme.error)
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
                    val fmt = NumberFormat.getCurrencyInstance(Locale.ITALY)
                    // UI-only state: which category card is expanded
                    var expandedCatId by rememberSaveable { mutableStateOf<String?>(null) }

                    LazyColumn(
                        modifier            = Modifier.fillMaxSize().padding(innerPadding),
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // ── Riepilogo totali ────────────────────────────────────
                        item {
                            Card(
                                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically,
                                    ) {
                                        Text("Spese del mese", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text       = fmt.format(state.totaleSpese),
                                            style      = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color      = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    if (state.totaleEntrate > 0) {
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            modifier              = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment     = Alignment.CenterVertically,
                                        ) {
                                            Text("Entrate del mese", style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                text       = fmt.format(state.totaleEntrate),
                                                style      = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color      = Color(0xFF2E7D32),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Per categoria ───────────────────────────────────────
                        item {
                            Text(
                                text  = "Per categoria",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(state.perCategoria, key = { it.categoriaId }) { cat ->
                            val isExpanded = expandedCatId == cat.categoriaId
                            val speseCategoria = remember(cat.categoriaId, state.spese) {
                                state.spese
                                    .filter { it.categoriaId == cat.categoriaId }
                                    .sortedByDescending { it.data?.seconds ?: 0 }
                            }
                            CardCategoria(
                                cat        = cat,
                                isExpanded = isExpanded,
                                onClick    = {
                                    expandedCatId = if (isExpanded) null else cat.categoriaId
                                },
                            )
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier            = Modifier.padding(start = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Spacer(Modifier.height(2.dp))
                                    speseCategoria.forEach { spesa ->
                                        RigaSpesaDettaglio(spesa = spesa, membri = state.membri)
                                    }
                                }
                            }
                        }

                        // ── Chi ha pagato (solo se >1 pagante) ──────────────────
                        if (state.perPagante.size > 1) {
                            item { Spacer(Modifier.height(4.dp)) }
                            item {
                                Text(
                                    text  = "Chi ha pagato",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            items(state.perPagante) { pagante ->
                                CardPagante(pagante = pagante)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardCategoria(
    cat: AnalisiMeseViewModel.CategoriaAnalisi,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val colore      = parseColore(cat.colore)
    val barColor    = if (cat.superaBudget) MaterialTheme.colorScheme.error else colore
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(colore))
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = cat.nome,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.weight(1f),
                )
                Text(
                    text       = NumberFormat.getCurrencyInstance(Locale.ITALY).format(cat.totale),
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (cat.superaBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector        = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                    modifier           = Modifier.size(20.dp),
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress   = { cat.percentualeBar },
                modifier   = Modifier.fillMaxWidth(),
                color      = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text  = "${cat.nSpese} ${if (cat.nSpese == 1) "spesa" else "spese"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text  = cat.percentualeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (cat.superaBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RigaSpesaDettaglio(spesa: Spesa, membri: List<Membro>) {
    val dataLabel = remember(spesa.data) {
        spesa.data?.toDate()?.let { date ->
            Instant.ofEpochMilli(date.time).atZone(ZoneId.systemDefault()).toLocalDate()
                .format(ofPattern("d MMM", Locale.ITALIAN))
        } ?: "${spesa.mese}/${spesa.anno}"
    }
    val paganteNome = remember(spesa.pagante, membri) {
        val m = membri.find { it.userId == spesa.pagante }
        m?.nominativoLocale?.ifBlank { null } ?: spesa.pagante.take(8)
    }
    val fmt = NumberFormat.getCurrencyInstance(Locale.ITALY)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = spesa.descrizione.ifBlank { "Spesa" },
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text  = "$dataLabel · $paganteNome",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!spesa.condivisa) {
                        SuggestionChip(
                            onClick = {},
                            label   = { Text("Personale", style = MaterialTheme.typography.labelSmall) },
                            colors  = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            ),
                            modifier = Modifier.height(18.dp),
                        )
                    }
                }
                if (spesa.note.isNotBlank()) {
                    Text(
                        text  = spesa.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text       = fmt.format(spesa.importo),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CardPagante(pagante: AnalisiMeseViewModel.PaganteAnalisi) {
    val colore = colorePagante(pagante.coloreIdx)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(colore))
                Spacer(Modifier.width(10.dp))
                Text(pagante.nome, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(
                    text       = NumberFormat.getCurrencyInstance(Locale.ITALY).format(pagante.totale),
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = colore,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress   = { pagante.percentualeBar },
                modifier   = Modifier.fillMaxWidth(),
                color      = colore,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text  = "${pagante.nSpese} ${if (pagante.nSpese == 1) "spesa" else "spese"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text  = "${NumberFormat.getCurrencyInstance(Locale.ITALY).format(pagante.totale)}  ·  ${(pagante.percentualeBar * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
