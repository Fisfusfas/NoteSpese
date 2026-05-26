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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.app.notespese.data.model.TipoSpesa
import com.app.notespese.ui.gruppi.parseColore
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
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
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }

                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Condivise") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Personali") })
                    }

                    when (selectedTab) {
                        0 -> TabCondivise(state = state)
                        1 -> TabPersonali(state = state)
                    }
                }
            }
        }
    }
}

// ── Tab Condivise ──────────────────────────────────────────────────────────────

@Composable
private fun TabCondivise(state: AnalisiMeseViewModel.UiState.Successo) {
    val spese  = remember(state.spese) { state.spese.filter { it.condivisa } }
    val totale = remember(spese) { spese.sumOf { it.importo } }
    val fmt    = NumberFormat.getCurrencyInstance(Locale.ITALY)

    if (spese.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text      = "Nessuna spesa condivisa questo mese.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(32.dp),
            )
        }
        return
    }

    var expandedCatId by rememberSaveable { mutableStateOf<String?>(null) }

    val perCategoria = remember(spese) {
        spese.groupBy { it.categoriaId }.map { (catId, gruppo) ->
            catId to gruppo
        }.sortedByDescending { it.second.sumOf { s -> s.importo } }
    }

    val perPagante = remember(spese) {
        spese.filter { it.pagante.isNotBlank() }
            .groupBy { it.pagante }
            .map { (userId, gruppo) -> userId to gruppo }
            .sortedByDescending { it.second.sumOf { s -> s.importo } }
    }

    val totaleFisse    = remember(spese) { spese.filter { it.tipo == TipoSpesa.FISSA.name }.sumOf { it.importo } }
    val totaleVariabili = remember(spese) { spese.filter { it.tipo != TipoSpesa.FISSA.name }.sumOf { it.importo } }
    val nFisse         = remember(spese) { spese.count { it.tipo == TipoSpesa.FISSA.name } }
    val nVariabili     = remember(spese) { spese.count { it.tipo != TipoSpesa.FISSA.name } }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Riepilogo
        item {
            RiepilogoCard(totaleSpese = totale, totaleEntrate = state.totaleEntrate, fmt = fmt)
        }

        // Per categoria
        item {
            Text("Per categoria", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(perCategoria, key = { it.first }) { (catId, gruppo) ->
            val cat = state.perCategoria.find { it.categoriaId == catId }
            if (cat != null) {
                val isExpanded = expandedCatId == catId
                CardCategoria(
                    cat        = cat,
                    isExpanded = isExpanded,
                    onClick    = { expandedCatId = if (isExpanded) null else catId },
                )
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier            = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Spacer(Modifier.height(2.dp))
                        gruppo.sortedByDescending { it.data?.seconds ?: 0 }.forEach { spesa ->
                            RigaSpesaDettaglio(spesa = spesa, membri = state.membri)
                        }
                    }
                }
            }
        }

        // Chi ha pagato
        if (perPagante.size > 1) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                Text("Chi ha pagato", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(perPagante.mapIndexed { idx, (userId, gruppo) ->
                val membro = state.membri.find { it.userId == userId }
                val nome   = membro?.nominativoLocale?.ifBlank { null } ?: userId.take(8)
                val tot    = gruppo.sumOf { it.importo }
                val perc   = if (totale > 0) (tot / totale).toFloat() else 0f
                AnalisiMeseViewModel.PaganteAnalisi(userId, nome, tot, gruppo.size, perc, idx)
            }) { pagante ->
                CardPagante(pagante = pagante)
            }
        }

        // Fisse vs Variabili
        item { Spacer(Modifier.height(4.dp)) }
        item {
            Text("Tipologia", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            CardTipologia(
                totaleFisse     = totaleFisse,
                totaleVariabili = totaleVariabili,
                nFisse          = nFisse,
                nVariabili      = nVariabili,
                totaleTot       = totale,
                fmt             = fmt,
            )
        }
    }
}

// ── Tab Personali ──────────────────────────────────────────────────────────────

@Composable
private fun TabPersonali(state: AnalisiMeseViewModel.UiState.Successo) {
    val spese = remember(state.spese) { state.spese.filter { !it.condivisa } }
    val fmt   = NumberFormat.getCurrencyInstance(Locale.ITALY)

    if (spese.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text      = "Nessuna spesa personale questo mese.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(32.dp),
            )
        }
        return
    }

    val perPagante = remember(spese) {
        spese.filter { it.pagante.isNotBlank() }
            .groupBy { it.pagante }
            .map { (userId, gruppo) ->
                val membro = state.membri.find { it.userId == userId }
                val nome   = membro?.nominativoLocale?.ifBlank { null } ?: userId.take(8)
                userId to (nome to gruppo)
            }
            .sortedByDescending { it.second.second.sumOf { s -> s.importo } }
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        perPagante.forEachIndexed { idx, (_, pair) ->
            val (nome, gruppo) = pair
            val totaleUtente   = gruppo.sumOf { it.importo }

            item(key = "header_$idx") {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier.size(10.dp).clip(CircleShape)
                            .background(colorePagante(idx))
                    )
                    Text(
                        text       = nome,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = colorePagante(idx),
                        modifier   = Modifier.weight(1f),
                    )
                    Text(
                        text       = fmt.format(totaleUtente),
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color      = colorePagante(idx),
                    )
                }
            }

            // Per categoria dell'utente
            val perCatUtente = gruppo.groupBy { it.categoriaId }
                .map { (catId, ss) -> catId to ss }
                .sortedByDescending { it.second.sumOf { s -> s.importo } }

            items(perCatUtente, key = { "${idx}_${it.first}" }) { (catId, ss) ->
                val catAnalisi = state.perCategoria.find { it.categoriaId == catId }
                val nomecat    = catAnalisi?.nome ?: "Senza categoria"
                val colorecat  = catAnalisi?.colore ?: "#9E9E9E"
                val tot        = ss.sumOf { it.importo }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(parseColore(colorecat)))
                        Spacer(Modifier.width(8.dp))
                        Text(nomecat, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text       = fmt.format(tot),
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))
                    Column(modifier = Modifier.padding(start = 28.dp, end = 14.dp, bottom = 8.dp)) {
                        ss.sortedByDescending { it.data?.seconds ?: 0 }.forEach { spesa ->
                            RigaSpesaDettaglio(spesa = spesa, membri = state.membri)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            if (idx < perPagante.size - 1) {
                item(key = "div_$idx") { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// ── Card tipologia fissa/variabile ─────────────────────────────────────────────

@Composable
private fun CardTipologia(
    totaleFisse: Double,
    totaleVariabili: Double,
    nFisse: Int,
    nVariabili: Int,
    totaleTot: Double,
    fmt: NumberFormat,
) {
    val percFisse     = if (totaleTot > 0) (totaleFisse / totaleTot).toFloat() else 0f
    val percVariabili = if (totaleTot > 0) (totaleVariabili / totaleTot).toFloat() else 0f
    val coloreFissa    = MaterialTheme.colorScheme.tertiary
    val coloreVariabile = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(coloreFissa))
                Text("Fisse", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("$nFisse op.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmt.format(totaleFisse), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            LinearProgressIndicator(
                progress   = { percFisse },
                modifier   = Modifier.fillMaxWidth(),
                color      = coloreFissa,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(coloreVariabile))
                Text("Variabili", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("$nVariabili op.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmt.format(totaleVariabili), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            LinearProgressIndicator(
                progress   = { percVariabili },
                modifier   = Modifier.fillMaxWidth(),
                color      = coloreVariabile,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

// ── Riepilogo card ─────────────────────────────────────────────────────────────

@Composable
private fun RiepilogoCard(totaleSpese: Double, totaleEntrate: Double, fmt: NumberFormat) {
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
                Text("Spese condivise", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text       = fmt.format(totaleSpese),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.error,
                )
            }
            if (totaleEntrate > 0) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("Entrate del mese", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text       = fmt.format(totaleEntrate),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF2E7D32),
                    )
                }
            }
        }
    }
}

// ── Card categoria ─────────────────────────────────────────────────────────────

@Composable
private fun CardCategoria(
    cat: AnalisiMeseViewModel.CategoriaAnalisi,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val colore   = parseColore(cat.colore)
    val barColor = if (cat.superaBudget) MaterialTheme.colorScheme.error else colore
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

// ── Riga spesa dettaglio ───────────────────────────────────────────────────────

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
                    if (spesa.tipo == TipoSpesa.FISSA.name) {
                        SuggestionChip(
                            onClick = {},
                            label   = { Text("Fissa", style = MaterialTheme.typography.labelSmall) },
                            colors  = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
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

// ── Card pagante ───────────────────────────────────────────────────────────────

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
