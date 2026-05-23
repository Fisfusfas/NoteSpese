package com.app.notespese.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.notespese.data.model.Utente
import com.app.notespese.ui.analisi.AnalisiEntrateScreen
import com.app.notespese.ui.analisi.AnalisiMeseScreen
import com.app.notespese.ui.budget.BudgetScreen
import com.app.notespese.ui.categorie.CategorieScreen
import com.app.notespese.ui.dashboard.DashboardScreen
import com.app.notespese.ui.entrate.AggiungiEntrataScreen
import com.app.notespese.ui.entrate.EntrataScreen
import com.app.notespese.ui.gruppi.CreaGruppoScreen
import com.app.notespese.ui.gruppi.ListaGruppiScreen
import com.app.notespese.ui.gruppi.impostazioni.ImpostazioniGruppoScreen
import com.app.notespese.ui.ricorrenze.AggiungiRicorrenzaScreen
import com.app.notespese.ui.ricorrenze.RicorrenzeScreen
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
                gruppoId             = gruppoId,
                onNavigateBack       = { navController.popBackStack() },
                onApriSpese          = { id -> navController.navigate(Screen.Spese.withId(id)) },
                onApriEntrate        = { id -> navController.navigate(Screen.Entrate.withId(id)) },
                onApriSaldi          = { id -> navController.navigate(Screen.Saldi.withId(id)) },
                onApriImpostazioni   = { id -> navController.navigate(Screen.ImpostazioniGruppo.withId(id)) },
                onApriAnalisi        = { gId, mese, anno ->
                    navController.navigate(Screen.AnalisiMese.withParams(gId, mese, anno))
                },
                onApriAnalisiEntrate = { gId, mese, anno ->
                    navController.navigate(Screen.AnalisiEntrateMese.withParams(gId, mese, anno))
                },
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
            AggiungiSpesaScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Modifica spesa ─────────────────────────────────────────────────────
        composable(
            route     = Screen.ModificaSpesa.route,
            arguments = listOf(
                navArgument("gruppoId") { type = NavType.StringType },
                navArgument("spesaId")  { type = NavType.StringType },
            )
        ) {
            AggiungiSpesaScreen(onNavigateBack = { navController.popBackStack() })
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
            AggiungiEntrataScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Modifica entrata ───────────────────────────────────────────────────
        composable(
            route     = Screen.ModificaEntrata.route,
            arguments = listOf(
                navArgument("gruppoId")  { type = NavType.StringType },
                navArgument("entrataId") { type = NavType.StringType },
            )
        ) {
            AggiungiEntrataScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Saldi ──────────────────────────────────────────────────────────────
        composable(
            route     = Screen.Saldi.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) {
            SaldoScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Analisi spese mese ─────────────────────────────────────────────────
        composable(
            route     = Screen.AnalisiMese.route,
            arguments = listOf(
                navArgument("gruppoId") { type = NavType.StringType },
                navArgument("mese")     { type = NavType.IntType },
                navArgument("anno")     { type = NavType.IntType },
            )
        ) {
            AnalisiMeseScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Analisi entrate mese ───────────────────────────────────────────────
        composable(
            route     = Screen.AnalisiEntrateMese.route,
            arguments = listOf(
                navArgument("gruppoId") { type = NavType.StringType },
                navArgument("mese")     { type = NavType.IntType },
                navArgument("anno")     { type = NavType.IntType },
            )
        ) {
            AnalisiEntrateScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Impostazioni gruppo ────────────────────────────────────────────────
        composable(
            route     = Screen.ImpostazioniGruppo.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: return@composable
            ImpostazioniGruppoScreen(
                onNavigateBack  = { navController.popBackStack() },
                onApriCategorie = { navController.navigate(Screen.Categorie.withId(gruppoId)) },
                onApriRicorrenze = { navController.navigate(Screen.Ricorrenze.withId(gruppoId)) },
                onApriBudget    = { navController.navigate(Screen.BudgetCategorie.withId(gruppoId)) },
            )
        }

        // ── Categorie ──────────────────────────────────────────────────────────
        composable(
            route     = Screen.Categorie.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) {
            CategorieScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Ricorrenze ─────────────────────────────────────────────────────────
        composable(
            route     = Screen.Ricorrenze.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: return@composable
            RicorrenzeScreen(
                onNavigateBack = { navController.popBackStack() },
                onAggiungi     = { navController.navigate(Screen.AggiungiRicorrenza.withId(gruppoId)) },
                onModifica     = { ricorrenzaId ->
                    navController.navigate(Screen.ModificaRicorrenza.withIds(gruppoId, ricorrenzaId))
                },
            )
        }

        // ── Aggiungi ricorrenza ────────────────────────────────────────────────
        composable(
            route     = Screen.AggiungiRicorrenza.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) {
            AggiungiRicorrenzaScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Modifica ricorrenza ────────────────────────────────────────────────
        composable(
            route     = Screen.ModificaRicorrenza.route,
            arguments = listOf(
                navArgument("gruppoId")     { type = NavType.StringType },
                navArgument("ricorrenzaId") { type = NavType.StringType },
            )
        ) {
            AggiungiRicorrenzaScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Budget categorie ───────────────────────────────────────────────────
        composable(
            route     = Screen.BudgetCategorie.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) {
            BudgetScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
