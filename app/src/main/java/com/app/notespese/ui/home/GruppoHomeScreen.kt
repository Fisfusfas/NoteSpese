package com.app.notespese.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.notespese.ui.dashboard.DashboardTabContent
import com.app.notespese.ui.dashboard.DashboardViewModel
import com.app.notespese.ui.entrate.EntrataListContent
import com.app.notespese.ui.entrate.EntrataViewModel
import com.app.notespese.ui.gruppi.iconaPerNome
import com.app.notespese.ui.gruppi.parseColore
import com.app.notespese.ui.saldi.SaldoTabContent
import com.app.notespese.ui.saldi.SaldoViewModel
import com.app.notespese.ui.spese.SpesaListContent
import com.app.notespese.ui.spese.SpesaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GruppoHomeScreen(
    gruppoId: String,
    onNavigateBack: () -> Unit,
    onApriImpostazioni: (String) -> Unit,
    onApriStatistiche: (String) -> Unit,
    onApriAggiungiSpesa: (String) -> Unit,
    onApriModificaSpesa: (String, String) -> Unit,
    onApriAggiungiEntrata: (String) -> Unit,
    onApriModificaEntrata: (String, String) -> Unit,
    onApriAnalisi: (String, Int, Int) -> Unit,
    onApriAnalisiEntrate: (String, Int, Int) -> Unit,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    spesaViewModel: SpesaViewModel = hiltViewModel(),
    entrataViewModel: EntrataViewModel = hiltViewModel(),
    saldoViewModel: SaldoViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val dashState   by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val spesaState  by spesaViewModel.uiState.collectAsStateWithLifecycle()
    val entrataState by entrataViewModel.uiState.collectAsStateWithLifecycle()
    val saldoState  by saldoViewModel.uiState.collectAsStateWithLifecycle()

    val gruppoNome = (dashState as? DashboardViewModel.UiState.Successo)?.gruppo?.nome ?: ""
    val gruppoColore = (dashState as? DashboardViewModel.UiState.Successo)
        ?.gruppo?.colore?.let { parseColore(it) } ?: MaterialTheme.colorScheme.primary
    val gruppoIcona = (dashState as? DashboardViewModel.UiState.Successo)
        ?.gruppo?.icona?.let { iconaPerNome(it) }

    // Bidirectional period sync — each VM's period propagates to all others.
    // setMese() is idempotent so cycles terminate after one round-trip.
    val dashMese = (dashState as? DashboardViewModel.UiState.Successo)?.mese
    val dashAnno = (dashState as? DashboardViewModel.UiState.Successo)?.anno
    LaunchedEffect(dashMese, dashAnno) {
        if (dashMese != null && dashAnno != null) {
            spesaViewModel.setMese(dashMese, dashAnno)
            entrataViewModel.setMese(dashMese, dashAnno)
            saldoViewModel.setMese(dashMese, dashAnno)
        }
    }
    val spesaMese = (spesaState as? SpesaViewModel.UiState.Successo)?.mese
    val spesaAnno = (spesaState as? SpesaViewModel.UiState.Successo)?.anno
    LaunchedEffect(spesaMese, spesaAnno) {
        if (spesaMese != null && spesaAnno != null) {
            dashboardViewModel.setMese(spesaMese, spesaAnno)
            entrataViewModel.setMese(spesaMese, spesaAnno)
            saldoViewModel.setMese(spesaMese, spesaAnno)
        }
    }
    val entrataMese = (entrataState as? EntrataViewModel.UiState.Successo)?.mese
    val entrataAnno = (entrataState as? EntrataViewModel.UiState.Successo)?.anno
    LaunchedEffect(entrataMese, entrataAnno) {
        if (entrataMese != null && entrataAnno != null) {
            dashboardViewModel.setMese(entrataMese, entrataAnno)
            spesaViewModel.setMese(entrataMese, entrataAnno)
            saldoViewModel.setMese(entrataMese, entrataAnno)
        }
    }
    val saldoMese = (saldoState as? SaldoViewModel.UiState.Successo)?.mese
    val saldoAnno = (saldoState as? SaldoViewModel.UiState.Successo)?.anno
    LaunchedEffect(saldoMese, saldoAnno) {
        if (saldoMese != null && saldoAnno != null) {
            dashboardViewModel.setMese(saldoMese, saldoAnno)
            spesaViewModel.setMese(saldoMese, saldoAnno)
            entrataViewModel.setMese(saldoMese, saldoAnno)
        }
    }

    val tabs = listOf(
        Triple("Home",     Icons.Default.Home,         0),
        Triple("Spese",    Icons.Default.ShoppingCart, 1),
        Triple("Entrate",  Icons.Default.TrendingUp,   2),
        Triple("Saldi",    Icons.Default.Balance,      3),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (gruppoIcona != null) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(gruppoColore)
                                    .padding(end = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector        = gruppoIcona,
                                    contentDescription = null,
                                    tint               = Color.White,
                                    modifier           = Modifier.size(16.dp),
                                )
                            }
                        }
                        Text(
                            text     = gruppoNome,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = if (gruppoIcona != null) 8.dp else 0.dp),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { onApriStatistiche(gruppoId) }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Statistiche")
                    }
                    IconButton(onClick = { onApriImpostazioni(gruppoId) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni gruppo")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        icon     = { Icon(icon, contentDescription = label) },
                        label    = { Text(label) },
                    )
                }
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                0, 1 -> FloatingActionButton(onClick = { onApriAggiungiSpesa(gruppoId) }) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi spesa")
                }
                2 -> FloatingActionButton(onClick = { onApriAggiungiEntrata(gruppoId) }) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi entrata")
                }
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            label = "tab_content",
        ) { tab ->
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> DashboardTabContent(
                        onApriAnalisi        = { m, a -> onApriAnalisi(gruppoId, m, a) },
                        onApriAnalisiEntrate = { m, a -> onApriAnalisiEntrate(gruppoId, m, a) },
                        viewModel            = dashboardViewModel,
                    )
                    1 -> SpesaListContent(
                        onModificaSpesa = { spesaId -> onApriModificaSpesa(gruppoId, spesaId) },
                        viewModel       = spesaViewModel,
                    )
                    2 -> EntrataListContent(
                        onAggiungiEntrata = { onApriAggiungiEntrata(gruppoId) },
                        onModificaEntrata = { entrataId -> onApriModificaEntrata(gruppoId, entrataId) },
                        viewModel         = entrataViewModel,
                    )
                    3 -> SaldoTabContent(viewModel = saldoViewModel)
                }
            }
        }
    }
}
