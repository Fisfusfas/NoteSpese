package com.app.notespese.widget

import android.content.Context
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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.app.notespese.MainActivity
import com.app.notespese.data.model.Spesa
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

class SpeseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = fetchData()
        provideContent {
            GlanceTheme {
                WidgetContent(data)
            }
        }
    }

    private suspend fun fetchData(): WidgetData {
        val userId = Firebase.auth.currentUser?.uid ?: return WidgetData()
        return try {
            val now  = YearMonth.now()
            val mese = now.monthValue
            val anno = now.year

            val gruppiDocs = Firebase.firestore.collection("gruppi")
                .whereArrayContains("membroIds", userId)
                .get().await().documents

            val gruppoDoc = gruppiDocs.firstOrNull() ?: return WidgetData()
            val gruppoId  = gruppoDoc.id
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

            WidgetData(
                nomeGruppo    = nomeGruppo,
                mese          = now.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ITALIAN)
                    .replaceFirstChar { it.uppercase() },
                totaleSpese   = totaleSpese,
                totaleEntrate = totaleEntrate,
                loaded        = true,
            )
        } catch (e: Exception) {
            WidgetData()
        }
    }
}

data class WidgetData(
    val nomeGruppo: String    = "",
    val mese: String          = "",
    val totaleSpese: Double   = 0.0,
    val totaleEntrate: Double = 0.0,
    val loaded: Boolean       = false,
)

@Composable
private fun WidgetContent(data: WidgetData) {
    val fmt     = NumberFormat.getCurrencyInstance(Locale.ITALY)
    val rosso   = Color(0xFFB71C1C)
    val verde   = Color(0xFF1B5E20)
    val surface = Color(0xFFF5F5F5)

    Column(
        modifier          = GlanceModifier
            .fillMaxSize()
            .background(surface)
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text  = "NoteSpese",
            style = TextStyle(fontSize = 11.sp, color = androidx.glance.unit.ColorProvider(Color(0xFF757575))),
        )
        if (!data.loaded) {
            Text(
                text  = "Apri l'app per caricare i dati",
                style = TextStyle(fontSize = 13.sp),
                modifier = GlanceModifier.padding(top = 4.dp),
            )
        } else {
            Text(
                text     = data.nomeGruppo,
                style    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.padding(top = 2.dp),
            )
            Text(
                text  = data.mese,
                style = TextStyle(fontSize = 11.sp, color = androidx.glance.unit.ColorProvider(Color(0xFF757575))),
            )
            Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("Spese", style = TextStyle(fontSize = 11.sp,
                        color = androidx.glance.unit.ColorProvider(Color(0xFF757575))))
                    Text(fmt.format(data.totaleSpese),
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = androidx.glance.unit.ColorProvider(rosso)))
                }
                Spacer(GlanceModifier.width(8.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("Entrate", style = TextStyle(fontSize = 11.sp,
                        color = androidx.glance.unit.ColorProvider(Color(0xFF757575))))
                    Text(fmt.format(data.totaleEntrate),
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = androidx.glance.unit.ColorProvider(verde)))
                }
            }
        }
    }
}
