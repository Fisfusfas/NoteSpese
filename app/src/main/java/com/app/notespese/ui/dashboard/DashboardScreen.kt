package com.app.notespese.ui.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.data.model.Spesa
import com.app.notespese.ui.gruppi.iconaPerNome
import com.app.notespese.ui.gruppi.parseColore
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    gruppoId: String,
    onNavigateBack: () -> Unit,
    onApriSpese: (String) -> Unit,
    onApriEntrate: (String) -> Unit,
    onApriSaldi: (String) -> Unit,
    onApriImpostazioni: (String) -> Unit,
    onApriAnalisi: (gruppoId: String, mese: Int, anno: Int) -> Unit,
    onApriAnalisiEntrate: (gruppoId: String, mese: Int, anno: Int) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is DashboardViewModel.UiState.Caricamento -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DashboardViewModel.UiState.Errore -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.messaggio, color = MaterialTheme.colorScheme.error)
            }
        }
        is DashboardViewModel.UiState.Successo -> {
            DashboardContent(
                state                = state,
                onNavigateBack       = onNavigateBack,
                onApriSpese          = { onApriSpese(gruppoId) },
                onApriEntrate        = { onApriEntrate(gruppoId) },
                onApriSaldi          = { onApriSaldi(gruppoId) },
                onApriImpostazioni   = { onApriImpostazioni(gruppoId) },
                onApriAnalisi        = { onApriAnalisi(gruppoId, state.mese, state.anno) },
                onApriAnalisiEntrate = { onApriAnalisiEntrate(gruppoId, state.mese, state.anno) },
                onMesePrecedente     = viewModel::mesePrecedente,
                onMeseSuccessivo     = viewModel::meseSuccessivo,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    state: DashboardViewModel.UiState.Successo,
    onNavigateBack: () -> Unit,
    onApriSpese: () -> Unit,
    onApriAnalisiEntrate: () -> Unit,
    onApriEntrate: () -> Unit,
    onApriSaldi: () -> Unit,
    onApriImpostazioni: () -> Unit,
    onApriAnalisi: () -> Unit,
    onMesePrecedente: () -> Unit,
    onMeseSuccessivo: () -> Unit,
) {
    val gruppoColore  = parseColore(state.gruppo.colore)
    val gruppoIcona   = iconaPerNome(state.gruppo.icona)
    val totaleSpese   = state.speseDelMese.sumOf { it.importo }
    val totaleEntrate = state.entrateDelMese.sumOf { it.importo }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(gruppoColore),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector        = gruppoIcona,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(18.dp),
                            )
                        }
                        Text(
                            text     = state.gruppo.nome,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = onApriImpostazioni) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni gruppo")
                    }
                },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {

            // ── Selettore mese ────────────────────────────────────────────────
            item {
                SelectoreMese(
                    mese             = state.mese,
                    anno             = state.anno,
                    onMesePrecedente = onMesePrecedente,
                    onMeseSuccessivo = onMeseSuccessivo,
                )
            }

            // ── Riepilogo ─────────────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CardRiepilogo(
                        modifier    = Modifier.weight(1f),
                        label       = "Spese del mese",
                        valore      = formatEuro(totaleSpese),
                        icona       = Icons.Default.ShoppingCart,
                        coloreIcona = MaterialTheme.colorScheme.primary,
                        onClick     = onApriAnalisi,
                    )
                    CardRiepilogo(
                        modifier    = Modifier.weight(1f),
                        label       = "Entrate del mese",
                        valore      = formatEuro(totaleEntrate),
                        icona       = Icons.Default.TrendingUp,
                        coloreIcona = Color(0xFF2E7D32),
                        onClick     = onApriAnalisiEntrate,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CardRiepilogo(
                        modifier    = Modifier.weight(1f),
                        label       = "Operazioni",
                        valore      = "${state.speseDelMese.size}",
                        icona       = Icons.Default.Receipt,
                        coloreIcona = MaterialTheme.colorScheme.tertiary,
                    )
                    CardRiepilogo(
                        modifier    = Modifier.weight(1f),
                        label       = "Membri",
                        valore      = "${state.membri.size}",
                        icona       = Icons.Default.Group,
                        coloreIcona = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Sezioni ───────────────────────────────────────────────────────
            item {
                Text(
                    text     = "Sezioni",
                    style    = MaterialTheme.typography.titleSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CardNavigazione(
                        modifier  = Modifier.weight(1f),
                        etichetta = "Spese",
                        icona     = Icons.Default.ShoppingCart,
                        colore    = gruppoColore,
                        onClick   = onApriSpese,
                    )
                    CardNavigazione(
                        modifier  = Modifier.weight(1f),
                        etichetta = "Entrate",
                        icona     = Icons.Default.TrendingUp,
                        colore    = Color(0xFF2E7D32),
                        onClick   = onApriEntrate,
                    )
                    CardNavigazione(
                        modifier  = Modifier.weight(1f),
                        etichetta = "Saldi",
                        icona     = Icons.Default.Balance,
                        colore    = Color(0xFF6A1B9A),
                        onClick   = onApriSaldi,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Ultime spese ──────────────────────────────────────────────────
            if (state.speseDelMese.isNotEmpty()) {
                item {
                    Text(
                        text     = "Ultime spese",
                        style    = MaterialTheme.typography.titleSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                items(state.speseDelMese.take(5)) { spesa ->
                    RowSpesa(spesa = spesa, onClick = onApriSpese)
                }
                if (state.speseDelMese.size > 5) {
                    item {
                        TextButton(
                            onClick  = onApriSpese,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            Text("Vedi tutte le ${state.speseDelMese.size} spese")
                        }
                    }
                }
            }
        }
    }
}

// ── Selettore mese ─────────────────────────────────────────────────────────────

@Composable
private fun SelectoreMese(
    mese: Int,
    anno: Int,
    onMesePrecedente: () -> Unit,
    onMeseSuccessivo: () -> Unit,
) {
    val etichetta = remember(mese, anno) {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN)
        LocalDate.of(anno, mese, 1).format(formatter).replaceFirstChar { it.uppercase() }
    }
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onMesePrecedente) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mese precedente")
        }
        Text(
            text       = etichetta,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onMeseSuccessivo) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mese successivo")
        }
    }
}

// ── Card riepilogo ─────────────────────────────────────────────────────────────

@Composable
private fun CardRiepilogo(
    label: String,
    valore: String,
    icona: ImageVector,
    coloreIcona: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        onClick  = onClick ?: {},
        enabled  = onClick != null,
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector        = icona,
                contentDescription = null,
                tint               = coloreIcona,
                modifier           = Modifier.size(20.dp),
            )
            Column {
                Text(
                    text       = valore,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Card navigazione ───────────────────────────────────────────────────────────

@Composable
private fun CardNavigazione(
    etichetta: String,
    icona: ImageVector,
    colore: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick  = onClick,
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = colore.copy(alpha = 0.12f)),
        shape    = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier              = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colore.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = icona,
                    contentDescription = null,
                    tint               = colore,
                    modifier           = Modifier.size(22.dp),
                )
            }
            Text(
                text       = etichetta,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color      = colore,
            )
        }
    }
}

// ── Riga spesa ─────────────────────────────────────────────────────────────────

@Composable
private fun RowSpesa(
    spesa: Spesa,
    onClick: () -> Unit,
) {
    val dataFormattata = remember(spesa.data) {
        spesa.data?.toDate()?.let { date ->
            val ld = Instant.ofEpochMilli(date.time)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN).format(ld)
        } ?: ""
    }
    ListItem(
        headlineContent = {
            Text(
                text     = spesa.descrizione.ifBlank { "Spesa" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = { if (dataFormattata.isNotEmpty()) Text(dataFormattata) },
        trailingContent   = {
            Text(
                text       = formatEuro(spesa.importo),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary,
            )
        },
        leadingContent    = {
            Icon(
                imageVector        = Icons.Default.Receipt,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier,
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

// ── Helper ─────────────────────────────────────────────────────────────────────

private fun formatEuro(importo: Double): String =
    NumberFormat.getCurrencyInstance(Locale.ITALY).format(importo)
