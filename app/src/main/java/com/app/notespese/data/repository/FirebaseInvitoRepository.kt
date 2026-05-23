package com.app.notespese.data.repository

import com.app.notespese.data.model.Invito
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.Ruolo
import com.app.notespese.data.model.StatoInvito
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FirebaseInvitoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : InvitoRepository {

    private fun invitiRef() = firestore.collection("inviti")
    private fun gruppiRef() = firestore.collection("gruppi")

    override suspend fun creaInvito(
        gruppoId: String,
        gruppoNome: String,
        creatoDa: String,
    ): Result<Invito> = runCatching {
        val codice   = generaCodice()
        val scadeIl  = Timestamp(Date(System.currentTimeMillis() + 48 * 60 * 60 * 1000L))
        val docRef   = invitiRef().document()
        val invito   = Invito(
            id         = docRef.id,
            gruppoId   = gruppoId,
            gruppoNome = gruppoNome,
            creatoDa   = creatoDa,
            codice     = codice,
            scadeIl    = scadeIl,
            stato      = StatoInvito.PENDING.name,
        )
        docRef.set(invito).await()
        invito
    }

    override suspend fun trovaCodice(codice: String): Result<Invito?> = runCatching {
        val snapshot = invitiRef()
            .whereEqualTo("codice", codice)
            .whereEqualTo("stato", StatoInvito.PENDING.name)
            .limit(1)
            .get()
            .await()
        snapshot.documents.firstOrNull()?.toObject(Invito::class.java)
    }

    /**
     * Transazione atomica:
     * 1. Verifica che l'invito sia ancora PENDING e non scaduto
     * 2. Aggiunge il membro al gruppo (subcollection + array)
     * 3. Segna l'invito come ACCETTATO
     */
    override suspend fun accettaInvito(invitoId: String, userId: String): Result<Unit> = runCatching {
        firestore.runTransaction { transaction ->
            val invitoRef  = invitiRef().document(invitoId)
            val invitoSnap = transaction.get(invitoRef)
            val invito     = invitoSnap.toObject(Invito::class.java)
                ?: error("Invito non trovato")

            if (invito.stato != StatoInvito.PENDING.name) error("Invito non più valido")
            val scade = invito.scadeIl?.toDate() ?: error("Data scadenza mancante")
            if (scade.before(Date())) error("Invito scaduto")

            val gruppoRef  = gruppiRef().document(invito.gruppoId)
            val membroRef  = gruppoRef.collection("membri").document(userId)
            val membro     = Membro(
                id              = userId,
                userId          = userId,
                ruolo           = Ruolo.MEMBRO.name,
                aggiuntoIl      = Timestamp.now(),
            )

            transaction.set(membroRef, membro)
            transaction.update(gruppoRef, "membroIds", FieldValue.arrayUnion(userId))
            transaction.update(invitoRef, mapOf(
                "stato"   to StatoInvito.ACCETTATO.name,
                "usatoDa" to userId,
            ))
        }.await()
    }

    override suspend fun rifiutaInvito(invitoId: String): Result<Unit> = runCatching {
        invitiRef().document(invitoId).update("stato", StatoInvito.RIFIUTATO.name).await()
    }

    /** Genera un codice alfanumerico uppercase a 8 caratteri. */
    private fun generaCodice(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // escludo 0,O,I,1 per leggibilità
        return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
