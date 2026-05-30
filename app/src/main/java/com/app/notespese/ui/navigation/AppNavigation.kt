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
import com.app.notespese.ui.grafici.GraficiScreen
import com.app.notespese.ui.categorie.CategorieScreen
import com.app.notespese.ui.entrate.AggiungiEntrataScreen
import com.app.notespese.ui.gruppi.CreaGruppoScreen
import com.app.notespese.ui.gruppi.ListaGruppiScreen
import com.app.notespese.ui.profilo.ProfiloScreen
import com.app.notespese.ui.gruppi.impostazioni.ImpostazioniGruppoScreen
import com.app.notespese.ui.home.GruppoHomeScreen
import com.app.notespese.ui.ricorrenze.AggiungiRicorrenzaScreen
import com.app.notespese.ui.ricorrenze.RicorrenzeScreen
import com.app.notespese.ui.spese.AggiungiSpesaScreen

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
                utente        = utente,
                onCreaGruppo  = { navController.navigate(Screen.CreaGruppo.route) },
                onApriGruppo  = { gruppoId -> navController.navigate(Screen.GruppoHome.withId(gruppoId)) },
                onApriProfilo = { navController.navigate(Screen.Profilo.route) },
                onSignOut     = onSignOut
            )
        }

        // ── Profilo utente ─────────────────────────────────────────────────────
        composable(Screen.Profilo.route) {
            ProfiloScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Crea gruppo ────────────────────────────────────────────────────────
        composable(Screen.CreaGruppo.route) {
            CreaGruppoScreen(
                onNavigateBack = { navController.popBackStack() },
                onGruppoCreato = { gruppoId ->
                    navController.navigate(Screen.GruppoHome.withId(gruppoId)) {
                        popUpTo(Screen.ListaGruppi.route)
                    }
                }
            )
        }

        // ── Gruppo home (bottom nav: Dashboard | Spese | Entrate | Saldi) ──────
        composable(
            route     = Screen.GruppoHome.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: return@composable
            GruppoHomeScreen(
                gruppoId              = gruppoId,
                onNavigateBack        = { navController.popBackStack() },
                onApriImpostazioni    = { id -> navController.navigate(Screen.ImpostazioniGruppo.withId(id)) },
                onApriStatistiche     = { id -> navController.navigate(Screen.Statistiche.withId(id)) },
                onApriAggiungiSpesa   = { id -> navController.navigate(Screen.AggiungiSpesa.withId(id)) },
                onApriModificaSpesa   = { gId, sId -> navController.navigate(Screen.ModificaSpesa.withIds(gId, sId)) },
                onApriAggiungiEntrata = { id -> navController.navigate(Screen.AggiungiEntrata.withId(id)) },
                onApriModificaEntrata = { gId, eId -> navController.navigate(Screen.ModificaEntrata.withIds(gId, eId)) },
                onApriAnalisi         = { gId, m, a -> navController.navigate(Screen.AnalisiMese.withParams(gId, m, a)) },
                onApriAnalisiEntrate  = { gId, m, a -> navController.navigate(Screen.AnalisiEntrateMese.withParams(gId, m, a)) },
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
                onNavigateBack   = { navController.popBackStack() },
                onApriCategorie  = { navController.navigate(Screen.Categorie.withId(gruppoId)) },
                onApriRicorrenze = { navController.navigate(Screen.Ricorrenze.withId(gruppoId)) },
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

        // ── Statistiche ────────────────────────────────────────────────────────
        composable(
            route     = Screen.Statistiche.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) {
            GraficiScreen(onNavigateBack = { navController.popBackStack() })
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

    }
}
