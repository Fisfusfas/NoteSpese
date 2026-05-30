package com.app.notespese.ui.gruppi

import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.data.model.Gruppo
import com.app.notespese.data.model.Utente

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaGruppiScreen(
    utente: Utente,
    onCreaGruppo: () -> Unit,
    onApriGruppo: (String) -> Unit,
    onApriProfilo: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ListaGruppiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-naviga al gruppo di default (gruppo unico o widget selezionato)
    LaunchedEffect(Unit) {
        viewModel.navigaToGruppo.collect { gruppoId -> onApriGruppo(gruppoId) }
    }

    // Naviga al gruppo dopo accettazione invito
    LaunchedEffect(viewModel.invitoAccettato) {
        viewModel.invitoAccettato?.let { gruppoId ->
            onApriGruppo(gruppoId)
            viewModel.resetInvito()
        }
    }

    var showInvitoDialog by rememberSaveable { mutableStateOf(false) }
    var codiceInput      by rememberSaveable { mutableStateOf("") }

    if (showInvitoDialog) {
        AlertDialog(
            onDismissRequest = {
                showInvitoDialog = false
                codiceInput = ""
                viewModel.resetInvito()
            },
            title = { Text("Entra con codice invito") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text  = "Inserisci il codice a 8 caratteri che ti è stato condiviso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value         = codiceInput,
                        onValueChange = { codiceInput = it.uppercase().take(8) },
                        label         = { Text("Codice invito") },
                        placeholder   = { Text("es. AB3C9XYZ") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        isError       = viewModel.erroreInvito != null,
                        supportingText = viewModel.erroreInvito?.let { { Text(it) } },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick  = { viewModel.entraConCodice(codiceInput) },
                    enabled  = codiceInput.length == 8 && !viewModel.cercandoInvito,
                ) {
                    if (viewModel.cercandoInvito) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Entra")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showInvitoDialog = false
                    codiceInput = ""
                    viewModel.resetInvito()
                }) { Text("Annulla") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("I miei gruppi")
                        Text(
                            text  = utente.nome,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showInvitoDialog = true }) {
                        Icon(
                            imageVector        = Icons.Default.VpnKey,
                            contentDescription = "Entra con codice",
                            tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    IconButton(onClick = onApriProfilo) {
                        Icon(
                            imageVector        = Icons.Default.AccountCircle,
                            contentDescription = "Profilo",
                            tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector        = Icons.Default.Logout,
                            contentDescription = "Esci",
                            tint               = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreaGruppo) {
                Icon(Icons.Default.Add, contentDescription = "Crea gruppo")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is ListaGruppiViewModel.UiState.Caricamento -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is ListaGruppiViewModel.UiState.Errore -> {
                    Text(
                        text      = state.messaggio,
                        modifier  = Modifier.align(Alignment.Center).padding(24.dp),
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.error
                    )
                }

                is ListaGruppiViewModel.UiState.Successo -> {
                    Column(Modifier.fillMaxSize()) {
                        if (utente.email.isBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text  = "Modalità demo",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        text  = "I dati non sono sincronizzati. Accedi con Google per salvarli.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                        if (state.gruppi.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                StatoVuoto()
                            }
                        } else {
                            LazyColumn(
                                modifier            = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding      = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 16.dp, vertical = 12.dp
                                )
                            ) {
                                items(state.gruppi, key = { it.id }) { gruppo ->
                                    GruppoCard(gruppo = gruppo, onClick = { onApriGruppo(gruppo.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GruppoCard(gruppo: Gruppo, onClick: () -> Unit) {
    val coloreGruppo = remember(gruppo.colore) { parseColore(gruppo.colore) }
    val iconaVector  = remember(gruppo.icona)  { iconaPerNome(gruppo.icona) }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier            = Modifier.padding(16.dp),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            // Cerchio colorato con icona
            Box(
                modifier        = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(coloreGruppo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = iconaVector,
                    contentDescription = null,
                    tint               = coloreGruppo,
                    modifier           = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = gruppo.nome,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                if (gruppo.descrizione.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text     = gruppo.descrizione,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Contatore membri
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Default.People,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text  = "${gruppo.membroIds.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatoVuoto(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector        = Icons.Default.People,
            contentDescription = null,
            modifier           = Modifier.size(64.dp),
            tint               = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text      = "Nessun gruppo ancora",
            style     = MaterialTheme.typography.titleMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text      = "Premi + per crearne uno\no accetta un invito",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun parseColore(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF1565C0)
}

fun iconaPerNome(nome: String): androidx.compose.ui.graphics.vector.ImageVector = when (nome) {
    "home"       -> Icons.Default.Home
    "work"       -> Icons.Default.Work
    "restaurant" -> Icons.Default.Restaurant
    "flight"     -> Icons.Default.Flight
    "shopping"   -> Icons.Default.ShoppingCart
    "car"        -> Icons.Default.DirectionsCar
    "pets"       -> Icons.Default.Pets
    "health"     -> Icons.Default.LocalHospital
    "school"     -> Icons.Default.School
    "group"      -> Icons.Default.Group
    "celebration"-> Icons.Default.Celebration
    else         -> Icons.Default.AccountBalanceWallet
}

// Catalogo icone per il picker
val ICONE_GRUPPO: List<Pair<String, ImageVector>> = listOf(
    "wallet"      to Icons.Default.AccountBalanceWallet,
    "home"        to Icons.Default.Home,
    "work"        to Icons.Default.Work,
    "restaurant"  to Icons.Default.Restaurant,
    "flight"      to Icons.Default.Flight,
    "shopping"    to Icons.Default.ShoppingCart,
    "car"         to Icons.Default.DirectionsCar,
    "pets"        to Icons.Default.Pets,
    "health"      to Icons.Default.LocalHospital,
    "school"      to Icons.Default.School,
    "group"       to Icons.Default.Group,
    "celebration" to Icons.Default.Celebration,
)

// Palette colori per il picker
val COLORI_GRUPPO = listOf(
    "#1565C0", "#2E7D32", "#C62828", "#E65100",
    "#6A1B9A", "#00695C", "#AD1457", "#4E342E",
    "#37474F", "#F57F17", "#0277BD", "#558B2F",
)
