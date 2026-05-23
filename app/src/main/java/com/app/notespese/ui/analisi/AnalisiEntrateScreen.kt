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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.app.notespese.data.model.Entrata
import com.app.notespese.data.model.Membro
import com.app.notespese.ui.gruppi.parseColore
import java.text.NumberFormat
import java.time.format.DateTimeFormatter.ofPattern
import java.util.Locale

private val PALETTE_PERSONE = listOf(
    Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFE65100),
    Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFFAD1457),
)
private fun colorePersona(idx: Int): Color = PALETTE_PERSONE[idx % PALETTE_PERSONE.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalisiEntrateScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalisiEntrateViewModel = hiltViewModel(),
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
                title = { Text("Entrate — $meseLabel") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is AnalisiEntrateViewModel.UiState.Caricamento ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is AnalisiEntrateViewModel.UiState.Errore ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.messaggio, color = MaterialTheme.colorScheme.error)
                }
            is AnalisiEntrateViewModel.UiState.Successo -> {
                if (state.perCategoria.isEmpty()) {
                    Box(
                        modifier         = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text      = "Nessuna entrata per questo mese.",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(32.dp),
                        )
                    }
                } else {
                    val fmt = NumberFormat.getCurrencyInstance(Locale.ITALY)
                    var expandedCatId by rememberSaveable { mutableStateOf<String?>(null) }

                    LazyColumn(
                        modifier            = Modifier.fillMaxSize().padding(innerPadding),
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // ── Riepilogo totale ────────────────────────────────────
                        item {
                            Card(
                                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier              = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically,
                                ) {
                                    Text("Entrate del mese", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text       = fmt.format(state.totale),
                                        style      = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color(0xFF2E7D32),
                                    )
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
                            val entrateCategoria = remember(cat.categoriaId, state.entrate) {
                                state.entrate
                                    .filter { it.categoriaId == cat.categoriaId }
                                    .sortedByDescending { it.importo }
                            }
                            CardCategoriaEntrata(
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
                                    entrateCategoria.forEach { entrata ->
                                        RigaEntrataDettaglio(entrata = entrata, membri = state.membri)
                                    }
                                }
                            }
                        }

                        // ── Chi ha ricevuto (solo se >1 persona) ───────────────
                        if (state.perPersona.size > 1) {
                            item { Spacer(Modifier.height(4.dp)) }
                            item {
                                Text(
                                    text  = "Chi ha ricevuto",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            items(state.perPersona) { persona ->
                                CardPersona(persona = persona)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardCategoriaEntrata(
    cat: AnalisiEntrateViewModel.CategoriaEntrataAnalisi,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val colore = parseColore(cat.colore)
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
                    color      = Color(0xFF2E7D32),
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
                color      = colore,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "${cat.nEntrate} ${if (cat.nEntrate == 1) "entrata" else "entrate"}  ·  ${cat.percentualeLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RigaEntrataDettaglio(entrata: Entrata, membri: List<Membro>) {
    val personaNome = remember(entrata.persona, membri) {
        val m = membri.find { it.userId == entrata.persona }
        m?.nominativoLocale?.ifBlank { null } ?: entrata.persona.take(8).ifBlank { "—" }
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
                    text       = personaNome,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                if (entrata.note.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text     = entrata.note,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text       = fmt.format(entrata.importo),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF2E7D32),
            )
        }
    }
}

@Composable
private fun CardPersona(persona: AnalisiEntrateViewModel.PersonaAnalisi) {
    val colore = colorePersona(persona.coloreIdx)
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
                Text(
                    persona.nome,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.weight(1f),
                )
                Text(
                    text       = NumberFormat.getCurrencyInstance(Locale.ITALY).format(persona.totale),
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = colore,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress   = { persona.percentualeBar },
                modifier   = Modifier.fillMaxWidth(),
                color      = colore,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "${persona.nEntrate} ${if (persona.nEntrate == 1) "entrata" else "entrate"}  ·  ${(persona.percentualeBar * 100).toInt()}% del tot.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
