package com.app.notespese.ui.gruppi.impostazioni

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Intent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpostazioniGruppoScreen(
    onNavigateBack: () -> Unit,
    onApriCategorie: () -> Unit,
    onApriRicorrenze: () -> Unit,
    viewModel: ImpostazioniGruppoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── Dialog invito ──────────────────────────────────────────────────────────
    viewModel.invitoCodice?.let { codice ->
        val clipboard  = LocalClipboardManager.current
        val context    = LocalContext.current
        val nomeGruppo = (uiState as? ImpostazioniGruppoViewModel.UiState.Successo)?.nomeGruppo ?: ""
        AlertDialog(
            onDismissRequest = viewModel::chiudiDialogCodice,
            title   = { Text("Codice invito generato") },
            text    = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text   = codice,
                        style  = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily    = FontFamily.Monospace,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 4.sp,
                        ),
                        color  = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = "Condividi questo codice con chi vuoi aggiungere. Scade tra 48 ore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboard.setText(AnnotatedString(codice))
                    viewModel.chiudiDialogCodice()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Copia e chiudi")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val testo = "Unisciti al gruppo \"$nomeGruppo\" su NoteSpese!\nCodice invito: $codice\nValido 48 ore."
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, testo)
                    }
                    context.startActivity(Intent.createChooser(intent, "Condividi codice invito"))
                    viewModel.chiudiDialogCodice()
                }) { Text("Condividi") }
            },
        )
    }

    // ── Dialog modifica nome ───────────────────────────────────────────────────
    if (viewModel.showEditNome) {
        AlertDialog(
            onDismissRequest = { viewModel.showEditNome = false },
            title   = { Text("Il tuo nome nel gruppo") },
            text    = {
                OutlinedTextField(
                    value         = viewModel.nuovoNome,
                    onValueChange = { viewModel.nuovoNome = it },
                    label         = { Text("Nome visualizzato") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick  = viewModel::salvaNome,
                    enabled  = viewModel.nuovoNome.isNotBlank() && !viewModel.salvandoNome,
                ) {
                    if (viewModel.salvandoNome) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Salva")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showEditNome = false }) { Text("Annulla") }
            },
        )
    }

    when (val state = uiState) {
        is ImpostazioniGruppoViewModel.UiState.Caricamento -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        is ImpostazioniGruppoViewModel.UiState.Errore -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.messaggio, color = MaterialTheme.colorScheme.error)
            }
        }
        is ImpostazioniGruppoViewModel.UiState.Successo -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Impostazioni — ${state.nomeGruppo}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {

                    // ── Gestione gruppo ───────────────────────────────────────
                    item {
                        Text(
                            text     = "Gestione gruppo",
                            style    = MaterialTheme.typography.titleSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            RigaNavigazione(
                                icona     = Icons.Default.Category,
                                etichetta = "Categorie e budget",
                                desc      = "Aggiungi, modifica categorie e imposta budget mensili",
                                onClick   = onApriCategorie,
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            RigaNavigazione(
                                icona     = Icons.Default.Repeat,
                                etichetta = "Ricorrenze",
                                desc      = "Spese che si ripetono ogni mese",
                                onClick   = onApriRicorrenze,
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            // ── Giorno inizio periodo ─────────────────────────
                            var sliderGiorno by remember(state.giornoInizioMese) {
                                mutableFloatStateOf(state.giornoInizioMese.toFloat())
                            }
                            ListItem(
                                headlineContent = { Text("Inizio periodo mensile", style = MaterialTheme.typography.bodyLarge) },
                                supportingContent = {
                                    Column {
                                        Text(
                                            text  = if (sliderGiorno.toInt() == 1) "Dal 1° del mese (mese solare)"
                                                    else "Dal giorno ${sliderGiorno.toInt()} al ${sliderGiorno.toInt() - 1} del mese successivo",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Slider(
                                            value              = sliderGiorno,
                                            onValueChange      = { sliderGiorno = it },
                                            onValueChangeFinished = { viewModel.salvaGiornoInizioMese(sliderGiorno.toInt()) },
                                            valueRange         = 1f..28f,
                                            steps              = 26,
                                        )
                                    }
                                },
                                leadingContent = {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            ListItem(
                                headlineContent   = { Text("Mostra nel widget", style = MaterialTheme.typography.bodyLarge) },
                                supportingContent = { Text("Questo gruppo appare nel widget home screen", style = MaterialTheme.typography.bodySmall) },
                                leadingContent    = { Icon(Icons.Default.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                trailingContent   = {
                                    Switch(
                                        checked         = state.widgetSelezionato,
                                        onCheckedChange = { viewModel.toggleWidget() },
                                    )
                                },
                            )
                        }
                    }

                    // ── Invita membro ─────────────────────────────────────────
                    item {
                        Text(
                            text     = "Invita membro",
                            style    = MaterialTheme.typography.titleSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text       = "Genera un codice a 8 caratteri valido 48 ore. Condividilo con chi vuoi aggiungere.",
                                    style      = MaterialTheme.typography.bodySmall,
                                    color      = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Spacer(Modifier.height(12.dp))
                                ElevatedButton(
                                    onClick  = viewModel::generaInvito,
                                    enabled  = !viewModel.generando,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    if (viewModel.generando) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.size(8.dp))
                                    } else {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(8.dp))
                                    }
                                    Text("Genera codice invito")
                                }
                                if (viewModel.erroreInvito != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text  = viewModel.erroreInvito!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }

                    // ── Lista membri ──────────────────────────────────────────
                    item {
                        Text(
                            text     = "Membri (${state.membri.size})",
                            style    = MaterialTheme.typography.titleSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    items(state.membri, key = { it.userId }) { membro ->
                        val nome   = membro.nominativoLocale.ifBlank { membro.userId.take(12) }
                        val isSelf = membro.userId == state.userId
                        ListItem(
                            headlineContent  = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(nome, fontWeight = if (isSelf) FontWeight.SemiBold else FontWeight.Normal)
                                    if (isSelf) {
                                        Spacer(Modifier.size(6.dp))
                                        Text(
                                            text  = "tu",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            },
                            supportingContent = { Text(membro.ruolo.lowercase().replaceFirstChar { it.uppercase() }) },
                            leadingContent    = {
                                Icon(Icons.Default.Person, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent   = if (isSelf) ({
                                IconButton(onClick = viewModel::apriEditNome) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifica nome",
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }) else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RigaNavigazione(
    icona: ImageVector,
    etichetta: String,
    desc: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent   = { Text(etichetta, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(desc, style = MaterialTheme.typography.bodySmall) },
        leadingContent    = { Icon(icona, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent   = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier          = Modifier.clickable(onClick = onClick),
    )
}
