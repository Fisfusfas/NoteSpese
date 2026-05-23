package com.app.notespese.data.repository

import com.app.notespese.data.model.Entrata
import com.app.notespese.data.model.MeseConfig
import com.app.notespese.data.model.Membro
import com.app.notespese.data.model.ModalitaSplit
import com.app.notespese.data.model.Saldo
import com.app.notespese.data.model.Spesa
import com.app.notespese.data.model.StatoCreditore
import com.app.notespese.data.model.StatoDebitore
import com.app.notespese.domain.usecase.CalcolaSaldiUseCase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSaldoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : SaldoRepository {

    private val calcolaUseCase = CalcolaSaldiUseCase()

    private fun gruppoRef(gruppoId: String) = firestore.collection("gruppi").document(gruppoId)
    private fun saldiRef(gruppoId: String, meseId: String) =
        gruppoRef(gruppoId).collection("mesi").document(meseId).collection("saldi")

    override fun osservaSaldi(gruppoId: String, meseId: String): Flow<List<Saldo>> = callbackFlow {
        val listener = saldiRef(gruppoId, meseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Saldo::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun osservaMeseConfig(gruppoId: String, meseId: String): Flow<MeseConfig?> = callbackFlow {
        val ref = gruppoRef(gruppoId).collection("mesi").document(meseId)
        val listener = ref.addSnapshotListener { snap, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snap?.toObject(MeseConfig::class.java))
        }
        awaitClose { listener.remove() }
    }

    override suspend fun impostaSplit(
        gruppoId: String,
        meseId: String,
        modalita: ModalitaSplit,
        pesi: Map<String, Double>,
    ): Result<Unit> = runCatching {
        val ref  = gruppoRef(gruppoId).collection("mesi").document(meseId)
        val snap = ref.get().await()
        val base = snap.toObject(MeseConfig::class.java) ?: MeseConfig(id = meseId)
        ref.set(base.copy(modalitaSplit = modalita.name, splitPersonalizzato = pesi)).await()
    }

    override suspend fun calcolaESalvaSaldi(gruppoId: String, meseId: String): Result<Unit> = runCatching {
        val (anno, mese) = meseId.split("-").let { it[0].toInt() to it[1].toInt() }
        val ref = gruppoRef(gruppoId)

        // 1. Leggi spese del mese
        val spese = ref.collection("spese")
            .whereEqualTo("anno", anno)
            .whereEqualTo("mese", mese)
            .get().await()
            .toObjects(Spesa::class.java)

        // 2. Leggi membri
        val membri = ref.collection("membri")
            .get().await()
            .toObjects(Membro::class.java)

        // 3. Leggi MeseConfig (opzionale)
        val meseDocRef  = ref.collection("mesi").document(meseId)
        val meseSnap    = meseDocRef.get().await()
        val meseConfig  = meseSnap.toObject(MeseConfig::class.java)

        // 4. Se DA_ENTRATE: calcola i pesi proporzionalmente alle entrate reali del mese
        //    coeff_A = entrate_A / totale_entrate
        val effectiveConfig = if (meseConfig?.modalitaSplit == ModalitaSplit.DA_ENTRATE.name) {
            val entrate = ref.collection("entrate")
                .whereEqualTo("anno", anno)
                .whereEqualTo("mese", mese)
                .get().await()
                .toObjects(Entrata::class.java)
            val totEnt = entrate.filter { it.persona.isNotBlank() }.sumOf { it.importo }
            val pesi: Map<String, Double> = if (totEnt > 0) {
                entrate
                    .filter { it.persona.isNotBlank() }
                    .groupBy { it.persona }
                    .mapValues { (_, lista) -> lista.sumOf { it.importo } / totEnt }
            } else emptyMap()
            meseConfig.copy(
                modalitaSplit       = ModalitaSplit.COEFFICIENTE.name,
                splitPersonalizzato = pesi,
            )
        } else meseConfig

        // 5. Calcola
        val nuoviSaldi = calcolaUseCase(spese, membri, effectiveConfig)

        // 6. Batch: elimina vecchi saldi, scrivi i nuovi
        val saldiCollRef = meseDocRef.collection("saldi")
        val vecchi = saldiCollRef.get().await()

        firestore.runBatch { batch ->
            vecchi.documents.forEach { batch.delete(it.reference) }
            nuoviSaldi.forEach { saldo -> batch.set(saldiCollRef.document(saldo.id), saldo) }
            // Crea MeseConfig se non esiste
            if (!meseSnap.exists()) batch.set(meseDocRef, MeseConfig(id = meseId))
        }.await()
    }

    override suspend fun segnaComePagato(gruppoId: String, meseId: String, saldoId: String): Result<Unit> = runCatching {
        saldiRef(gruppoId, meseId).document(saldoId).update(
            mapOf(
                "statoDebitore" to StatoDebitore.PAGATO.name,
                "dataPagamento" to Timestamp.now(),
            )
        ).await()
    }

    override suspend fun confermaPagamento(gruppoId: String, meseId: String, saldoId: String): Result<Unit> = runCatching {
        saldiRef(gruppoId, meseId).document(saldoId).update(
            mapOf(
                "statoCreditore" to StatoCreditore.CONFERMATO.name,
                "dataConferma"   to Timestamp.now(),
            )
        ).await()
    }
}
