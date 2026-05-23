package com.app.notespese.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Documento: gruppi/{gruppoId}/mesi/{meseId}/saldi/{coppia}
 * Il documento id è "userId1_userId2" con userId ordinati alfabeticamente.
 *
 * Ciclo di vita:
 * debitore segna PAGATO → notifica al creditore
 * creditore segna CONFERMATO → saldo SALDATO (entrambi IN stato finale)
 */
data class Saldo(
    @DocumentId val id: String = "",
    val da: String = "",
    val a: String = "",
    val importoCalcolato: Double = 0.0,
    val statoDebitore: String = StatoDebitore.IN_ATTESA.name,
    val statoCreditore: String = StatoCreditore.IN_ATTESA.name,
    val dataPagamento: Timestamp? = null,
    val dataConferma: Timestamp? = null,
    val note: String = "",
) {
    val isSaldato: Boolean
        get() = statoDebitore == StatoDebitore.PAGATO.name &&
                statoCreditore == StatoCreditore.CONFERMATO.name

    companion object {
        /** Genera l'id coppia ordinando alfabeticamente i due userId. */
        fun coppiaId(userId1: String, userId2: String): String {
            return if (userId1 < userId2) "${userId1}_${userId2}"
            else "${userId2}_${userId1}"
        }
    }
}
