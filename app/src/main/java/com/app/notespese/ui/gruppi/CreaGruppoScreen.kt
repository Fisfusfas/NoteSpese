package com.app.notespese.ui.gruppi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreaGruppoScreen(
    onNavigateBack: () -> Unit,
    onGruppoCreato: (String) -> Unit,
    viewModel: CreaGruppoViewModel = hiltViewModel()
) {
    val stato by viewModel.stato.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(stato) {
        when (val s = stato) {
            is CreaGruppoViewModel.StatoCreazione.Successo -> {
                onGruppoCreato(s.gruppoId)
                viewModel.resetStato()
            }
            is CreaGruppoViewModel.StatoCreazione.Errore -> {
                snackbarHostState.showSnackbar(s.messaggio)
                viewModel.resetStato()
            }
            else -> Unit
        }
    }

    val isLoading = stato is CreaGruppoViewModel.StatoCreazione.Caricamento

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuovo gruppo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {

            // ── Anteprima ─────────────────────────────────────────────────────
            AnteprimaGruppo(
                nome   = viewModel.nome,
                icona  = viewModel.icona,
                colore = viewModel.colore,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(24.dp))

            // ── Nome ──────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = viewModel.nome,
                onValueChange = { viewModel.aggiornaNome(it) },
                label         = { Text("Nome gruppo *") },
                isError       = viewModel.nomeToccato && !viewModel.nomeValido,
                supportingText = {
                    if (viewModel.nomeToccato && !viewModel.nomeValido)
                        Text("Il nome è obbligatorio", color = MaterialTheme.colorScheme.error)
                },
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) viewModel.segnaomeToccato() }
            )

            Spacer(Modifier.height(12.dp))

            // ── Descrizione ───────────────────────────────────────────────────
            OutlinedTextField(
                value         = viewModel.descrizione,
                onValueChange = { viewModel.aggiornaDescrizione(it) },
                label         = { Text("Descrizione") },
                maxLines      = 3,
                modifier      = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // ── Picker icona ──────────────────────────────────────────────────
            Text("Icona", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement   = Arrangement.spacedBy(10.dp)
            ) {
                ICONE_GRUPPO.forEach { (chiave, vettore) ->
                    val selezionata = chiave == viewModel.icona
                    val colore = parseColore(viewModel.colore)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (selezionata) colore.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (selezionata) 2.dp else 0.dp,
                                color = if (selezionata) colore else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.aggiornaIcona(chiave) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = vettore,
                            contentDescription = chiave,
                            tint               = if (selezionata) colore
                                                 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Picker colore ─────────────────────────────────────────────────
            Text("Colore", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement   = Arrangement.spacedBy(10.dp)
            ) {
                COLORI_GRUPPO.forEach { hex ->
                    val selezionato = hex == viewModel.colore
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parseColore(hex))
                            .border(
                                width = if (selezionato) 3.dp else 0.dp,
                                color = if (selezionato) MaterialTheme.colorScheme.outline
                                        else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.aggiornaColore(hex) }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Bottone crea ──────────────────────────────────────────────────
            Button(
                onClick   = { viewModel.creaGruppo() },
                enabled   = !isLoading,
                modifier  = Modifier.fillMaxWidth().height(52.dp),
                shape     = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Crea gruppo", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AnteprimaGruppo(
    nome: String,
    icona: String,
    colore: String,
    modifier: Modifier = Modifier
) {
    val coloreP = parseColore(colore)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier         = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(coloreP.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = iconaPerNome(icona),
                contentDescription = null,
                tint               = coloreP,
                modifier           = Modifier.size(38.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text  = nome.ifBlank { "Nome gruppo" },
            style = MaterialTheme.typography.titleMedium,
            color = if (nome.isBlank()) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}
