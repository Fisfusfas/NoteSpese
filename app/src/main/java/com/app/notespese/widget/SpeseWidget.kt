package com.app.notespese.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.app.notespese.MainActivity
import com.app.notespese.data.model.Spesa
import com.app.notespese.data.repository.WidgetPreferenceRepository
import com.app.notespese.ui.quick.QuickSpesaActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

class SpeseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = fetchData(context)
        provideContent {
            GlanceTheme {
                WidgetContent(context, data)
            }
        }
    }

    private suspend fun fetchData(context: Context): WidgetData {
        val userId = Firebase.auth.currentUser?.uid ?: return WidgetData()
        return try {
            val now   = YearMonth.now()
            val mese  = now.monthValue
            val anno  = now.year

            val widgetPrefs = WidgetPreferenceRepository(context)
            val savedGruppoId = widgetPrefs.getWidgetGruppoId()

            val gruppiDocs = Firebase.firestore.collection("gruppi")
                .whereArrayContains("membroIds", userId)
                .get().await().documents

            val gruppoDoc = if (savedGruppoId.isNotEmpty()) {
                gruppiDocs.find { it.id == savedGruppoId } ?: gruppiDocs.firstOrNull()
            } else {
                gruppiDocs.firstOrNull()
            } ?: return WidgetData()

            val gruppoId   = gruppoDoc.id
            val nomeGruppo = gruppoDoc.getString("nome") ?: ""

            val spese = Firebase.firestore.collection("gruppi").document(gruppoId)
                .collection("spese")
                .whereEqualTo("mese", mese).whereEqualTo("anno", anno)
                .get().await().toObjects(Spesa::class.java)

            val entrate = Firebase.firestore.collection("gruppi").document(gruppoId)
                .collection("entrate")
                .whereEqualTo("mese", mese).whereEqualTo("anno", anno)
                .get().await()

            val totaleSpese   = spese.sumOf { it.importo }
            val totaleEntrate = entrate.sumOf { it.getDouble("importo") ?: 0.0 }

            val recenti = spese
                .sortedByDescending { it.data?.toDate() }
                .take(3)
                .map { SpesaRecente(it.descrizione.ifBlank { "Spesa" }, it.importo) }

            WidgetData(
                gruppoId      = gruppoId,
                nomeGruppo    = nomeGruppo,
                mese          = now.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ITALIAN)
                    .replaceFirstChar { it.uppercase() } + " ${anno}",
                totaleSpese   = totaleSpese,
                totaleEntrate = totaleEntrate,
                recenti       = recenti,
                loaded        = true,
            )
        } catch (e: Exception) {
            WidgetData()
        }
    }
}

data class SpesaRecente(val descrizione: String, val importo: Double)

data class WidgetData(
    val gruppoId: String            = "",
    val nomeGruppo: String          = "",
    val mese: String                = "",
    val totaleSpese: Double         = 0.0,
    val totaleEntrate: Double       = 0.0,
    val recenti: List<SpesaRecente> = emptyList(),
    val loaded: Boolean             = false,
)

@Composable
private fun WidgetContent(context: Context, data: WidgetData) {
    val fmt      = NumberFormat.getCurrencyInstance(Locale.ITALY)
    val rosso    = Color(0xFFB71C1C)
    val verde    = Color(0xFF1B5E20)
    val surface  = Color(0xFFF5F5F5)
    val hint     = Color(0xFF757575)
    val divider  = Color(0xFFE0E0E0)
    val primary  = Color(0xFF1565C0)
    val onPrimary = Color.White

    val mainIntent = if (data.gruppoId.isNotEmpty()) {
        Intent(context, QuickSpesaActivity::class.java).apply {
            putExtra("gruppoId", data.gruppoId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    } else null

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surface)
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier          = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text  = "NoteSpese",
                style = TextStyle(fontSize = 11.sp, color = ColorProvider(primary), fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (data.loaded) {
                Text(
                    text  = data.mese,
                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(hint)),
                )
            }
        }

        if (!data.loaded) {
            Spacer(GlanceModifier.height(12.dp))
            Text(
                text  = "Apri l'app per aggiornare il widget",
                style = TextStyle(fontSize = 13.sp, color = ColorProvider(hint)),
            )
            return@Column
        }

        Text(
            text     = data.nomeGruppo,
            style    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(top = 2.dp),
        )

        Spacer(GlanceModifier.height(12.dp))

        // ── Totals row ──────────────────────────────────────────────────────
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text  = "Spese",
                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(hint)),
                )
                Text(
                    text  = fmt.format(data.totaleSpese),
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorProvider(rosso)),
                )
            }
            Box(
                modifier = GlanceModifier.width(1.dp).height(40.dp).background(divider),
                contentAlignment = Alignment.Center,
            ) {}
            Spacer(GlanceModifier.width(12.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text  = "Entrate",
                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(hint)),
                )
                Text(
                    text  = fmt.format(data.totaleEntrate),
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorProvider(verde)),
                )
            }
        }

        if (data.recenti.isNotEmpty()) {
            Spacer(GlanceModifier.height(10.dp))
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(divider)) {}
            Spacer(GlanceModifier.height(8.dp))

            Text(
                text  = "Ultime spese",
                style = TextStyle(fontSize = 11.sp, color = ColorProvider(hint), fontWeight = FontWeight.Medium),
            )
            Spacer(GlanceModifier.height(6.dp))

            data.recenti.forEach { spesa ->
                Row(
                    modifier          = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = spesa.descrizione,
                        style    = TextStyle(fontSize = 13.sp),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        text  = fmt.format(spesa.importo),
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ColorProvider(rosso)),
                    )
                }
            }
        }

        Spacer(GlanceModifier.defaultWeight())

        // ── Aggiungi spesa button ────────────────────────────────────────────
        val btnAction = if (mainIntent != null) actionStartActivity(mainIntent)
                        else actionStartActivity<MainActivity>()
        Box(
            modifier         = GlanceModifier
                .fillMaxWidth()
                .background(primary)
                .clickable(btnAction)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = "+",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorProvider(onPrimary)),
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text  = "Aggiungi spesa",
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ColorProvider(onPrimary)),
                )
            }
        }
    }
}
