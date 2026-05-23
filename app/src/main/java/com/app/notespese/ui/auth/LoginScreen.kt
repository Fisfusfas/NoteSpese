package com.app.notespese.ui.auth

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Schermata di login. Non gestisce navigazione: la MainActivity osserva
 * lo stesso AuthViewModel e sostituisce questa schermata quando lo stato
 * diventa Autenticato.
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val signInLoading by viewModel.signInLoading.collectAsStateWithLifecycle()
    val signInError  by viewModel.signInError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current as Activity

    LaunchedEffect(uiState) {
        if (uiState is AuthViewModel.UiState.Errore) {
            snackbarHostState.showSnackbar(
                (uiState as AuthViewModel.UiState.Errore).messaggio
            )
        }
    }

    LaunchedEffect(signInError) {
        signInError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSignInError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier             = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            verticalArrangement  = Arrangement.Center,
            horizontalAlignment  = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector        = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(80.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text  = "NoteSpese",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text      = "Gestisci le spese condivise\ncon semplicità",
                style     = MaterialTheme.typography.bodyLarge,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            OutlinedButton(
                onClick  = { viewModel.signInWithGoogle(activity) },
                enabled  = !signInLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (signInLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Accedi con Google")
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick  = { viewModel.signInAnonymously() },
                enabled  = !signInLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continua come demo")
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text      = "La modalità demo usa Firebase anonimo.\nI dati non vengono sincronizzati.",
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}
