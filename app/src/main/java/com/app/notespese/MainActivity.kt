package com.app.notespese

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

    // ViewModel condiviso: LoginScreen usa hiltViewModel() che risolve
    // la stessa istanza dallo Activity ViewModelStore.
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
