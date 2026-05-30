package com.app.notespese.ui.quick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.ui.common.CategoriaSelector
import com.app.notespese.ui.theme.NoteSpeseTema
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickSpesaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            NoteSpeseTema {
                val viewModel: QuickSpesaViewModel = hiltViewModel()
                LaunchedEffect(viewModel.esito) {
                    if (viewModel.esito is QuickSpesaViewModel.Esito.Salvato) finish()
                }
                QuickSpesaBottomSheet(viewModel = viewModel, onClose = ::finish)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickSpesaBottomSheet(
    viewModel: QuickSpesaViewModel,
    onClose: () -> Unit,
) {
    val categorie by viewModel.categorie.collectAsStateWithLifecycle()
    val membri    by viewModel.membri.collectAsStateWithLifecycle()
    var paganteExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimming overlay — tap to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .clickable(onClick = onClose),
        )

        // Sheet content anchored to bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp),
                    ),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text("Aggiungi spesa", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi")
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Importo ────────────────────────────────────────────────────────
            OutlinedTextField(
                value           = viewModel.importoText,
                onValueChange   = { viewModel.importoText = it; viewModel.erroreImporto = false },
                label           = { Text("Importo *") },
                prefix          = { Text("€") },
                singleLine      = true,
                isError         = viewModel.erroreImporto,
                supportingText  = if (viewModel.erroreImporto) {{ Text("Inserisci un importo valido") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier        = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            // ── Descrizione ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = viewModel.descrizione,
                onValueChange = { viewModel.descrizione = it },
                label         = { Text("Descrizione") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            // ── Categoria ──────────────────────────────────────────────────────
            CategoriaSelector(
                categorie              = categorie,
                categoriaSelezionataId = viewModel.categoriaId,
                onSeleziona            = { viewModel.categoriaId = it },
                onCreaCategoria        = { _, _, _ -> },
            )

            Spacer(Modifier.height(8.dp))

            // ── Chi paga ───────────────────────────────────────────────────────
            if (membri.size > 1) {
                ExposedDropdownMenuBox(
                    expanded         = paganteExpanded,
                    onExpandedChange = { paganteExpanded = it },
                ) {
                    val labelPagante = membri.find { it.userId == viewModel.pagante }
                        ?.nominativoLocale?.ifBlank { null }
                        ?: "Seleziona chi paga"
                    OutlinedTextField(
                        value         = labelPagante,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Chi paga") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paganteExpanded) },
                        modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded         = paganteExpanded,
                        onDismissRequest = { paganteExpanded = false },
                    ) {
                        membri.forEach { membro ->
                            val nome = membro.nominativoLocale.ifBlank { membro.userId.take(10) }
                            DropdownMenuItem(
                                text    = { Text(nome) },
                                onClick = { viewModel.pagante = membro.userId; paganteExpanded = false },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Condivisa ──────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Spesa condivisa", style = MaterialTheme.typography.bodyLarge)
                    Text("Verrà divisa tra i membri", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = viewModel.condivisa, onCheckedChange = { viewModel.condivisa = it })
            }

            // ── Tipo fissa ─────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Spesa fissa", style = MaterialTheme.typography.bodyLarge)
                    Text("Bolletta, affitto, abbonamento…", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = viewModel.tipoFissa, onCheckedChange = { viewModel.tipoFissa = it })
            }

            Spacer(Modifier.height(16.dp))

            // ── Salva ──────────────────────────────────────────────────────────
            Button(
                onClick  = viewModel::salva,
                enabled  = viewModel.esito !is QuickSpesaViewModel.Esito.Caricamento,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (viewModel.esito is QuickSpesaViewModel.Esito.Caricamento) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salva spesa")
                }
            }

            if (viewModel.esito is QuickSpesaViewModel.Esito.Errore) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = (viewModel.esito as QuickSpesaViewModel.Esito.Errore).msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
