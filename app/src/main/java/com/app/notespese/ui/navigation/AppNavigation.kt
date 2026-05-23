package com.app.notespese.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.notespese.data.model.Utente
import com.app.notespese.ui.dashboard.DashboardScreen
import com.app.notespese.ui.entrate.AggiungiEntrataScreen
import com.app.notespese.ui.entrate.EntrataScreen
import com.app.notespese.ui.gruppi.CreaGruppoScreen
import com.app.notespese.ui.gruppi.ListaGruppiScreen
import com.app.notespese.ui.saldi.SaldoScreen
import com.app.notespese.ui.spese.AggiungiSpesaScreen
import com.app.notespese.ui.spese.SpesaScreen

@Composable
fun AppNavigation(
    utente: Utente,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Screen.ListaGruppi.route
    ) {

        // ── Lista gruppi ───────────────────────────────────────────────────────
        composable(Screen.ListaGruppi.route) {
            ListaGruppiScreen(
                utente       = utente,
                onCreaGruppo = { navController.navigate(Screen.CreaGruppo.route) },
                onApriGruppo = { gruppoId -> navController.navigate(Screen.Dashboard.withId(gruppoId)) },
                onSignOut    = onSignOut
            )
        }

        // ── Crea gruppo ────────────────────────────────────────────────────────
        composable(Screen.CreaGruppo.route) {
            CreaGruppoScreen(
                onNavigateBack = { navController.popBackStack() },
                onGruppoCreato = { gruppoId ->
                    navController.navigate(Screen.Dashboard.withId(gruppoId)) {
                        popUpTo(Screen.ListaGruppi.route)
                    }
                }
            )
        }

        // ── Dashboard gruppo ───────────────────────────────────────────────────
        composable(
            route     = Screen.Dashboard.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: return@composable
            DashboardScreen(
                gruppoId           = gruppoId,
                onNavigateBack     = { navController.popBackStack() },
                onApriSpese        = { id -> navController.navigate(Screen.Spese.withId(id)) },
                onApriEntrate      = { id -> navController.navigate(Screen.Entrate.withId(id)) },
                onApriSaldi        = { id -> navController.navigate(Screen.Saldi.withId(id)) },
                onApriDebiti       = { id -> navController.navigate(Screen.Debiti.withId(id)) },
                onApriImpostazioni = { id -> navController.navigate(Screen.ImpostazioniGruppo.withId(id)) },
            )
        }

        // ── Spese ──────────────────────────────────────────────────────────────
        composable(
            route     = Screen.Spese.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) {
            SpesaScreen(
                onNavigateBack  = { navController.popBackStack() },
                onAggiungiSpesa = { id -> navController.navigate(Screen.AggiungiSpesa.withId(id)) },
                onModificaSpesa = { gruppoId, spesaId ->
                    navController.navigate(Screen.ModificaSpesa.withIds(gruppoId, spesaId))
                },
            )
        }

        // ── Aggiungi spesa ─────────────────────────────────────────────────────
        composable(
            route     = Screen.AggiungiSpesa.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) {
            AggiungiSpesaScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Modifica spesa ─────────────────────────────────────────────────────
        composable(
            route     = Screen.ModificaSpesa.route,
            arguments = listOf(
                navArgument("gruppoId") { type = NavType.StringType },
                navArgument("spesaId")  { type = NavType.StringType },
            )
        ) {
            AggiungiSpesaScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Entrate ────────────────────────────────────────────────────────────
        composable(
            route     = Screen.Entrate.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) {
            EntrataScreen(
                onNavigateBack    = { navController.popBackStack() },
                onAggiungiEntrata = { id -> navController.navigate(Screen.AggiungiEntrata.withId(id)) },
                onModificaEntrata = { gruppoId, entrataId ->
                    navController.navigate(Screen.ModificaEntrata.withIds(gruppoId, entrataId))
                },
            )
        }

        // ── Aggiungi entrata ───────────────────────────────────────────────────
        composable(
            route     = Screen.AggiungiEntrata.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) {
            AggiungiEntrataScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Modifica entrata ───────────────────────────────────────────────────
        composable(
            route     = Screen.ModificaEntrata.route,
            arguments = listOf(
                navArgument("gruppoId")   { type = NavType.StringType },
                navArgument("entrataId")  { type = NavType.StringType },
            )
        ) {
            AggiungiEntrataScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Saldi ──────────────────────────────────────────────────────────────
        composable(
            route     = Screen.Saldi.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) {
            SaldoScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Debiti ─────────────────────────────────────────────────────────────
        composable(
            route     = Screen.Debiti.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) {
            Text("Debiti — placeholder")
        }

        // ── Impostazioni gruppo ────────────────────────────────────────────────
        composable(
            route     = Screen.ImpostazioniGruppo.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) {
            Text("Impostazioni gruppo — placeholder")
        }
    }
}
