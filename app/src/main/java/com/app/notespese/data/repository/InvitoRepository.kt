package com.app.notespese.data.repository

import com.app.notespese.data.model.Invito

interface InvitoRepository {

    /** Crea un invito con codice casuale a 8 caratteri, scade tra 48 ore. */
    suspend fun creaInvito(gruppoId: String, gruppoNome: String, creatoDa: String): Result<Invito>

    /** Cerca l'invito tramite il codice a 8 caratteri inserito dall'utente. */
    suspend fun trovaCodice(codice: String): Result<Invito?>

    /** Accetta l'invito: aggiunge l'utente al gruppo e segna l'invito come ACCETTATO. */
    suspend fun accettaInvito(invitoId: String, userId: String): Result<Unit>

    suspend fun rifiutaInvito(invitoId: String): Result<Unit>
}
