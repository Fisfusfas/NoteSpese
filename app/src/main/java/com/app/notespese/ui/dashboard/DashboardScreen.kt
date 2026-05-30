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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Standalone screen (with own Scaffold) ─────────────────────────────────────

@Composable
fun DashboardScreen(
    gruppoId: String,
    onNavigateBack: () -> Unit,
    onApriSpese: (String) -> Unit,
    onApriEntrate: (String) -> Unit,
    onApriSaldi: (String) -> Unit,
    onApriImpostazioni: (String) -> Unit,
    onApriGrafici: (String) -> Unit,
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
            DashboardFullContent(
                state                = state,
                onNavigateBack       = onNavigateBack,
                onApriSpese          = { onApriSpese(gruppoId) },
                onApriEntrate        = { onApriEntrate(gruppoId) },
                onApriSaldi          = { onApriSaldi(gruppoId) },
                onApriImpostazioni   = { onApriImpostazioni(gruppoId) },
                onApriGrafici        = { onApriGrafici(gruppoId) },
                onApriAnalisi        = { onApriAnalisi(gruppoId, state.mese, state.anno) },
                onApriAnalisiEntrate = { onApriAnalisiEntrate(gruppoId, state.mese, state.anno) },
                onMesePrecedente     = viewModel::mesePrecedente,
                onMeseSuccessivo     = viewModel::meseSuccessivo,
            )
        }
    }
}

// ── Tab content (no Scaffold, used inside GruppoHomeScreen) ───────────────────

@Composable
fun DashboardTabContent(
    onApriAnalisi: (Int, Int) -> Unit,
    onApriAnalisiEntrate: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is DashboardViewModel.UiState.Caricamento -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DashboardViewModel.UiState.Errore -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.messaggio, color = MaterialTheme.colorScheme.error)
            }
        }
        is DashboardViewModel.UiState.Successo -> {
            DashboardPageContent(
                state                = state,
                modifier             = modifier,
                onApriAnalisi        = { onApriAnalisi(state.mese, state.anno) },
                onApriAnalisiEntrate = { onApriAnalisiEntrate(state.mese, state.anno) },
                onMesePrecedente     = viewModel::mesePrecedente,
                onMeseSuccessivo     = viewModel::meseSuccessivo,
            )
        }
    }
}

// ── Shared page content ────────────────────────────────────────────────────────

@Composable
private fun DashboardPageContent(
    state: DashboardViewModel.UiState.Successo,
    modifier: Modifier = Modifier,
    onApriAnalisi: () -> Unit,
    onApriAnalisiEntrate: () -> Unit,
    onMesePrecedente: () -> Unit,
    onMeseSuccessivo: () -> Unit,
) {
    val totaleSpese    = state.speseDelMese.sumOf { it.importo }
    val totaleEntrate  = state.entrateDelMese.sumOf { it.importo }
    val saldo          = totaleEntrate - totaleSpese
    val residuoTotale  = state.totaleEntrateTotali - state.totaleSpeseTotali
    val coloreVerde    = Color(0xFF2E7D32)

    LazyColumn(
        modifier       = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {

        // ── Selettore periodo ─────────────────────────────────────────────────
        item {
            SelectoreMese(
                etichetta        = state.periodoLabel,
                onMesePrecedente = onMesePrecedente,
                onMeseSuccessivo = onMeseSuccessivo,
            )
        }

        // ── Riepilogo ─────────────────────────────────────────────────────────
        item {
            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardRiepilogo(
                        modifier    = Modifier.weight(1f),
                        label       = "Spese",
                        valore      = formatEuro(totaleSpese),
                        icona       = Icons.Default.ShoppingCart,
                        coloreIcona = MaterialTheme.colorScheme.error,
                        onClick     = onApriAnalisi,
                    )
                    CardRiepilogo(
                        modifier    = Modifier.weight(1f),
                        label       = "Entrate",
                        valore      = formatEuro(totaleEntrate),
                        icona       = Icons.Default.TrendingUp,
                        coloreIcona = coloreVerde,
                        onClick     = onApriAnalisiEntrate,
                    )
                }

                // Saldo netto del periodo
                CardSaldoRiga(
                    label  = "Saldo del periodo",
                    valore = saldo,
                    coloreVerde = coloreVerde,
                )

                // Residuo lifetime (tutte le entrate - tutte le spese)
                CardSaldoRiga(
                    label  = "Residuo totale",
                    valore = residuoTotale,
                    coloreVerde = coloreVerde,
                    icona = Icons.Default.AccountBalanceWallet,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Ultime spese ──────────────────────────────────────────────────────
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
                RowSpesa(spesa = spesa, onClick = onApriAnalisi)
            }
            if (state.speseDelMese.size > 5) {
                item {
                    TextButton(
                        onClick  = onApriAnalisi,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) {
                        Text("Vedi tutte le ${state.speseDelMese.size} spese")
                    }
                }
            }
        }
    }
}

// ── Standalone full content (with Scaffold) ───────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardFullContent(
    state: DashboardViewModel.UiState.Successo,
    onNavigateBack: () -> Unit,
    onApriSpese: () -> Unit,
    onApriEntrate: () -> Unit,
    onApriSaldi: () -> Unit,
    onApriImpostazioni: () -> Unit,
    onApriGrafici: () -> Unit,
    onApriAnalisi: () -> Unit,
    onApriAnalisiEntrate: () -> Unit,
    onMesePrecedente: () -> Unit,
    onMeseSuccessivo: () -> Unit,
) {
    val gruppoColore = parseColore(state.gruppo.colore)
    val gruppoIcona  = iconaPerNome(state.gruppo.icona)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier         = Modifier.size(32.dp).clip(CircleShape).background(gruppoColore),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(gruppoIcona, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Text(state.gruppo.nome, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = onApriGrafici) {
                        Icon(Icons.Default.BarChart, contentDescription = "Statistiche")
                    }
                    IconButton(onClick = onApriImpostazioni) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni gruppo")
                    }
                },
            )
        }
    ) { innerPadding ->
        DashboardPageContent(
            state                = state,
            modifier             = Modifier.padding(innerPadding),
            onApriAnalisi        = onApriAnalisi,
            onApriAnalisiEntrate = onApriAnalisiEntrate,
            onMesePrecedente     = onMesePrecedente,
            onMeseSuccessivo     = onMeseSuccessivo,
        )
    }
}

// ── Selettore periodo ─────────────────────────────────────────────────────────

@Composable
private fun SelectoreMese(
    etichetta: String,
    onMesePrecedente: () -> Unit,
    onMeseSuccessivo: () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onMesePrecedente) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Periodo precedente")
        }
        Text(etichetta, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onMeseSuccessivo) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Periodo successivo")
        }
    }
}

// ── Card saldo riga ───────────────────────────────────────────────────────────

@Composable
private fun CardSaldoRiga(
    label: String,
    valore: Double,
    coloreVerde: Color,
    icona: ImageVector = if (valore >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = if (valore >= 0)
                coloreVerde.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector        = icona,
                    contentDescription = null,
                    tint               = if (valore >= 0) coloreVerde else MaterialTheme.colorScheme.error,
                    modifier           = Modifier.size(20.dp),
                )
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text       = formatEuro(valore),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = if (valore >= 0) coloreVerde else MaterialTheme.colorScheme.error,
            )
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
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icona, contentDescription = null, tint = coloreIcona, modifier = Modifier.size(20.dp))
            Column {
                Text(valore, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Riga spesa ─────────────────────────────────────────────────────────────────

@Composable
private fun RowSpesa(spesa: Spesa, onClick: () -> Unit) {
    val dataFormattata = remember(spesa.data) {
        spesa.data?.toDate()?.let { date ->
            val ld = Instant.ofEpochMilli(date.time).atZone(ZoneId.systemDefault()).toLocalDate()
            DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN).format(ld)
        } ?: ""
    }
    ListItem(
        headlineContent   = { Text(spesa.descrizione.ifBlank { "Spesa" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { if (dataFormattata.isNotEmpty()) Text(dataFormattata) },
        trailingContent   = {
            Text(
                text       = formatEuro(spesa.importo),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary,
            )
        },
        leadingContent    = { Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier          = Modifier,
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

// ── Helper ─────────────────────────────────────────────────────────────────────

private fun formatEuro(importo: Double): String =
    NumberFormat.getCurrencyInstance(Locale.ITALY).format(importo)
