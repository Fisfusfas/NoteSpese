package com.app.notespese.ui.profilo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfiloScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfiloViewModel = hiltViewModel(),
) {
    val utente by viewModel.utente.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(utente?.nome) {
        utente?.nome?.let { viewModel.inizializza(it) }
    }

    LaunchedEffect(viewModel.salvato) {
        if (viewModel.salvato) {
            snackbarHostState.showSnackbar("Profilo aggiornato")
            viewModel.salvato = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Il mio profilo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Avatar ─────────────────────────────────────────────────────────
            val fotoUrl = utente?.fotoUrl
            if (fotoUrl != null) {
                AsyncImage(
                    model               = fotoUrl,
                    contentDescription  = "Foto profilo",
                    contentScale        = ContentScale.Crop,
                    modifier            = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier         = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = (utente?.nome?.firstOrNull() ?: "?").uppercaseChar().toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            utente?.email?.takeIf { it.isNotBlank() }?.let { email ->
                Text(
                    text  = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Nickname ───────────────────────────────────────────────────────
            Text(
                text      = "Nome visualizzato",
                style     = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier  = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text     = "Questo nome sarà visibile a tutti i tuoi gruppi.",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value         = viewModel.editNickname,
                onValueChange = { viewModel.editNickname = it; viewModel.errore = null },
                label         = { Text("Nome / Nickname") },
                singleLine    = true,
                isError       = viewModel.errore != null,
                supportingText = viewModel.errore?.let { { Text(it) } },
                modifier      = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick  = viewModel::salva,
                enabled  = !viewModel.salvando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (viewModel.salvando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salva")
                }
            }
        }
    }
}
