package com.app.notespese.domain.usecase

import com.app.notespese.data.model.MeseConfig
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.ModalitaSplit
import com.app.notespese.data.model.Saldo
import com.app.notespese.data.model.Spesa
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Debt simplification: dato l'insieme di spese condivise del mese,
 * calcola il set minimo di transazioni per saldare tutti i debiti.
 */
class CalcolaSaldiUseCase {

    operator fun invoke(
        spese: List<Spesa>,
        membri: List<Membro>,
        meseConfig: MeseConfig?,
    ): List<Saldo> {
        if (membri.size < 2) return emptyList()

        val memberIds   = membri.map { it.userId }
        val modalita    = meseConfig?.modalitaSplit ?: ModalitaSplit.COEFFICIENTE.name
        val splitCustom = meseConfig?.splitPersonalizzato ?: emptyMap()

        // Bilancio netto per ogni membro (positivo = creditore, negativo = debitore)
        val bilanci = mutableMapOf<String, Double>().apply { memberIds.forEach { put(it, 0.0) } }

        for (spesa in spese.filter { it.condivisa && it.pagante.isNotBlank() }) {
            if (!bilanci.containsKey(spesa.pagante)) continue
            val quote = calcolaQuote(spesa.importo, memberIds, modalita, splitCustom)
            bilanci[spesa.pagante] = bilanci[spesa.pagante]!! + spesa.importo
            quote.forEach { (uid, quota) -> bilanci[uid] = bilanci[uid]!! - quota }
        }

        // Arrotonda a 2 decimali per eliminare drift floating-point
        val bilanciFinal = bilanci.mapValues { (_, v) -> arrotonda(v) }

        return semplificaDebiti(bilanciFinal)
    }

    // ── Split ──────────────────────────────────────────────────────────────────

    private fun calcolaQuote(
        importo: Double,
        memberIds: List<String>,
        modalita: String,
        splitCustom: Map<String, Double>,
    ): Map<String, Double> {
        if (modalita == ModalitaSplit.CINQUANTA.name || splitCustom.isEmpty()) {
            val quota = importo / memberIds.size
            return memberIds.associateWith { quota }
        }
        // PERSONALIZZATO / COEFFICIENTE con pesi custom
        val totalePesi = splitCustom.values.sum().takeIf { it > 0.0 } ?: 1.0
        return memberIds.associateWith { uid ->
            val peso = splitCustom[uid] ?: (totalePesi / memberIds.size)
            importo * peso / totalePesi
        }
    }

    // ── Debt simplification (greedy) ───────────────────────────────────────────

    private fun semplificaDebiti(bilanci: Map<String, Double>): List<Saldo> {
        val creditori = bilanci.filter { it.value >  0.01 }.toMutableMap()
        val debitori  = bilanci.filter { it.value < -0.01 }.toMutableMap()
        val saldi     = mutableListOf<Saldo>()

        while (creditori.isNotEmpty() && debitori.isNotEmpty()) {
            val (idCreditore, credito) = creditori.maxBy { it.value }
            val (idDebitore,  debito)  = debitori.minBy { it.value }   // più negativo

            val importo = minOf(credito, abs(debito))
            val importoArrot = arrotonda(importo)

            if (importoArrot >= 0.01) {
                saldi.add(
                    Saldo(
                        id               = Saldo.coppiaId(idDebitore, idCreditore),
                        da               = idDebitore,
                        a                = idCreditore,
                        importoCalcolato = importoArrot,
                    )
                )
            }

            val nuovoCredito = arrotonda(credito - importo)
            val nuovoDebito  = arrotonda(debito  + importo)

            if (nuovoCredito < 0.01) creditori.remove(idCreditore) else creditori[idCreditore] = nuovoCredito
            if (nuovoDebito  > -0.01) debitori.remove(idDebitore)  else debitori[idDebitore]  = nuovoDebito
        }

        return saldi
    }

    private fun arrotonda(v: Double): Double = (v * 100).roundToLong() / 100.0
}
