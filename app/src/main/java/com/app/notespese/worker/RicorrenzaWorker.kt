package com.app.notespese.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.notespese.data.model.Ricorrenza
import com.app.notespese.data.model.Spesa
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.time.YearMonth
import java.time.ZoneId

@HiltWorker
class RicorrenzaWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = auth.currentUser?.uid ?: return Result.success()

        val now  = YearMonth.now()
        val mese = now.monthValue
        val anno = now.year

        val gruppi = firestore.collection("gruppi")
            .whereArrayContains("membroIds", userId)
            .get().await().documents

        for (gruppoDoc in gruppi) {
            val gruppoId = gruppoDoc.id

            val ricorrenze = firestore.collection("gruppi").document(gruppoId)
                .collection("ricorrenze")
                .whereEqualTo("attiva", true)
                .get().await()
                .toObjects(Ricorrenza::class.java)

            for (ric in ricorrenze) {
                val spesaId  = "ric_${ric.id}_${anno}_${mese}"
                val spesaRef = firestore.collection("gruppi").document(gruppoId)
                    .collection("spese").document(spesaId)

                if (spesaRef.get().await().exists()) continue

                val maxGiorno = now.lengthOfMonth()
                val giorno    = ric.giornoDelMese.coerceIn(1, maxGiorno)
                val data      = java.time.LocalDate.of(anno, mese, giorno)
                val instant   = data.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val timestamp = Timestamp(instant.epochSecond, 0)

                val spesa = Spesa(
                    id          = spesaId,
                    importo     = ric.importo,
                    descrizione = ric.descrizione,
                    categoriaId = ric.categoriaId,
                    pagante     = ric.pagante,
                    condivisa   = ric.condivisa,
                    tipo        = ric.tipo,
                    data        = timestamp,
                    mese        = mese,
                    anno        = anno,
                    note        = "",
                )
                spesaRef.set(spesa).await()
            }
        }

        return Result.success()
    }
}
