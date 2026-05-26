package com.app.notespese

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.ui.auth.AuthViewModel
import com.app.notespese.ui.auth.LoginScreen
import com.app.notespese.ui.navigation.AppNavigation
import com.app.notespese.ui.theme.NoteSpeseTema
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — no-op, NotificationHelper checks permission at send time */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            NoteSpeseTema {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

                    when (val state = uiState) {
                        is AuthViewModel.UiState.Caricamento -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        is AuthViewModel.UiState.NonAutenticato,
                        is AuthViewModel.UiState.Errore -> {
                            LoginScreen()
                        }

                        is AuthViewModel.UiState.Autenticato -> {
                            AppNavigation(
                                utente    = state.utente,
                                onSignOut = { authViewModel.signOut() }
                            )
                        }
                    }
                }
            }
        }
    }
}
