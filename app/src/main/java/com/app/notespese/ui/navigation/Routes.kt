package com.app.notespese.ui.navigation

sealed class Screen(val route: String) {
    // ── Auth ──────────────────────────────────────────────────────────────────
    data object Login : Screen("login")

    // ── App principale ────────────────────────────────────────────────────────
    data object ListaGruppi  : Screen("lista_gruppi")
    data object CreaGruppo   : Screen("crea_gruppo")
    data object AccettaInvito: Screen("accetta_invito")

    // ── Gruppo (riceve gruppoId come path param) ───────────────────────────────
    data object Dashboard         : Screen("gruppo/{gruppoId}/dashboard") {
        fun withId(id: String) = "gruppo/$id/dashboard"
    }
    data object Spese             : Screen("gruppo/{gruppoId}/spese") {
        fun withId(id: String) = "gruppo/$id/spese"
    }
    data object Entrate           : Screen("gruppo/{gruppoId}/entrate") {
        fun withId(id: String) = "gruppo/$id/entrate"
    }
    data object Saldi             : Screen("gruppo/{gruppoId}/saldi") {
        fun withId(id: String) = "gruppo/$id/saldi"
    }
    data object Debiti            : Screen("gruppo/{gruppoId}/debiti") {
        fun withId(id: String) = "gruppo/$id/debiti"
    }
    data object ImpostazioniGruppo: Screen("gruppo/{gruppoId}/impostazioni") {
        fun withId(id: String) = "gruppo/$id/impostazioni"
    }
    data object AggiungiSpesa     : Screen("gruppo/{gruppoId}/spese/aggiungi") {
        fun withId(id: String) = "gruppo/$id/spese/aggiungi"
    }
}
