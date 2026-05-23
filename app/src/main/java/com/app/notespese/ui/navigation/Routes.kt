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
    data object ImpostazioniGruppo: Screen("gruppo/{gruppoId}/impostazioni") {
        fun withId(id: String) = "gruppo/$id/impostazioni"
    }
    data object Categorie : Screen("gruppo/{gruppoId}/categorie") {
        fun withId(id: String) = "gruppo/$id/categorie"
    }
    data object Ricorrenze : Screen("gruppo/{gruppoId}/ricorrenze") {
        fun withId(id: String) = "gruppo/$id/ricorrenze"
    }
    data object AggiungiRicorrenza : Screen("gruppo/{gruppoId}/ricorrenze/aggiungi") {
        fun withId(id: String) = "gruppo/$id/ricorrenze/aggiungi"
    }
    data object ModificaRicorrenza : Screen("gruppo/{gruppoId}/ricorrenze/{ricorrenzaId}/modifica") {
        fun withIds(gruppoId: String, ricorrenzaId: String) = "gruppo/$gruppoId/ricorrenze/$ricorrenzaId/modifica"
    }
    data object AggiungiSpesa   : Screen("gruppo/{gruppoId}/spese/aggiungi") {
        fun withId(id: String) = "gruppo/$id/spese/aggiungi"
    }
    data object ModificaSpesa   : Screen("gruppo/{gruppoId}/spese/{spesaId}/modifica") {
        fun withIds(gruppoId: String, spesaId: String) = "gruppo/$gruppoId/spese/$spesaId/modifica"
    }
    data object AggiungiEntrata : Screen("gruppo/{gruppoId}/entrate/aggiungi") {
        fun withId(id: String) = "gruppo/$id/entrate/aggiungi"
    }
    data object ModificaEntrata : Screen("gruppo/{gruppoId}/entrate/{entrataId}/modifica") {
        fun withIds(gruppoId: String, entrataId: String) = "gruppo/$gruppoId/entrate/$entrataId/modifica"
    }
    data object AnalisiMese : Screen("gruppo/{gruppoId}/analisi/{mese}/{anno}") {
        fun withParams(gruppoId: String, mese: Int, anno: Int) = "gruppo/$gruppoId/analisi/$mese/$anno"
    }
    data object AnalisiEntrateMese : Screen("gruppo/{gruppoId}/analisi-entrate/{mese}/{anno}") {
        fun withParams(gruppoId: String, mese: Int, anno: Int) = "gruppo/$gruppoId/analisi-entrate/$mese/$anno"
    }
}
