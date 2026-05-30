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
import com.app.notespese.ui.gruppi.iconaPerNome
import com.app.notespese.ui.gruppi.parseColore
import com.app.notespese.ui.saldi.SaldoTabContent
import com.app.notespese.ui.spese.SpesaListContent

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
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val dashState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val gruppoNome = (dashState as? DashboardViewModel.UiState.Successo)?.gruppo?.nome ?: ""
    val gruppoColore = (dashState as? DashboardViewModel.UiState.Successo)
        ?.gruppo?.colore?.let { parseColore(it) } ?: MaterialTheme.colorScheme.primary
    val gruppoIcona = (dashState as? DashboardViewModel.UiState.Successo)
        ?.gruppo?.icona?.let { iconaPerNome(it) }

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
                    if (selectedTab == 0) {
                        IconButton(onClick = { onApriStatistiche(gruppoId) }) {
                            Icon(Icons.Default.BarChart, contentDescription = "Statistiche")
                        }
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
            if (selectedTab <= 1) {
                FloatingActionButton(onClick = { onApriAggiungiSpesa(gruppoId) }) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi spesa")
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
                    )
                    2 -> EntrataListContent(
                        onAggiungiEntrata = { onApriAggiungiEntrata(gruppoId) },
                        onModificaEntrata = { entrataId -> onApriModificaEntrata(gruppoId, entrataId) },
                    )
                    3 -> SaldoTabContent()
                }
            }
        }
    }
}
