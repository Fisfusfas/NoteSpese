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
import androidx.glance.layout.size
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
            val now  = YearMonth.now()
            val mese = now.monthValue
            val anno = now.year

            val widgetPrefs   = WidgetPreferenceRepository(context)
            val savedGruppoId = widgetPrefs.getWidgetGruppoId()

            val gruppiDocs = Firebase.firestore.collection("gruppi")
                .whereArrayContains("membroIds", userId)
                .get().await().documents

            val gruppoDoc = (if (savedGruppoId.isNotEmpty())
                gruppiDocs.find { it.id == savedGruppoId } ?: gruppiDocs.firstOrNull()
            else
                gruppiDocs.firstOrNull()) ?: return WidgetData()

            val gruppoId   = gruppoDoc.id
            val nomeGruppo = gruppoDoc.getString("nome") ?: ""

            val spese = Firebase.firestore.collection("gruppi").document(gruppoId)
                .collection("spese")
                .whereEqualTo("mese", mese).whereEqualTo("anno", anno)
                .get().await().toObjects(Spesa::class.java)

            val entrateDocs = Firebase.firestore.collection("gruppi").document(gruppoId)
                .collection("entrate")
                .whereEqualTo("mese", mese).whereEqualTo("anno", anno)
                .get().await()

            val categorieDocs = Firebase.firestore.collection("gruppi").document(gruppoId)
                .collection("categorie").get().await().documents

            val budgetDocs = Firebase.firestore.collection("gruppi").document(gruppoId)
                .collection("budget").get().await().documents

            val totaleSpese   = spese.sumOf { it.importo }
            val totaleEntrate = entrateDocs.sumOf { it.getDouble("importo") ?: 0.0 }

            val perCategoria = spese
                .groupBy { it.categoriaId }
                .map { (catId, gruppo) ->
                    val catDoc  = categorieDocs.find { it.id == catId }
                    val budgDoc = budgetDocs.find { it.id == catId }
                    CategoriaWidget(
                        nome    = catDoc?.getString("nome") ?: "Senza categoria",
                        colore  = catDoc?.getString("colore") ?: "#9E9E9E",
                        totale  = gruppo.sumOf { it.importo },
                        budget  = budgDoc?.getDouble("importoMensile") ?: 0.0,
                    )
                }
                .sortedByDescending { it.totale }
                .take(5)

            WidgetData(
                gruppoId      = gruppoId,
                nomeGruppo    = nomeGruppo,
                mese          = now.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ITALIAN)
                    .replaceFirstChar { it.uppercase() } + " $anno",
                totaleSpese   = totaleSpese,
                totaleEntrate = totaleEntrate,
                perCategoria  = perCategoria,
                loaded        = true,
            )
        } catch (e: Exception) {
            WidgetData()
        }
    }
}

data class CategoriaWidget(
    val nome: String,
    val colore: String,
    val totale: Double,
    val budget: Double,
)

data class WidgetData(
    val gruppoId: String                  = "",
    val nomeGruppo: String                = "",
    val mese: String                      = "",
    val totaleSpese: Double               = 0.0,
    val totaleEntrate: Double             = 0.0,
    val perCategoria: List<CategoriaWidget> = emptyList(),
    val loaded: Boolean                   = false,
)

@Composable
private fun WidgetContent(context: Context, data: WidgetData) {
    val fmt = NumberFormat.getCurrencyInstance(Locale.ITALY)
    val isDark = (context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    val surface   = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val onSurface = if (isDark) Color(0xFFE0E0E0) else Color(0xFF212121)
    val hint      = if (isDark) Color(0xFF9E9E9E) else Color(0xFF757575)
    val divider   = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE0E0E0)
    val primary   = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)
    val onPrimary = if (isDark) Color(0xFF0D1B2A) else Color.White
    val rosso     = if (isDark) Color(0xFFEF9A9A) else Color(0xFFB71C1C)
    val verde     = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)

    val openIntent = if (data.gruppoId.isNotEmpty()) {
        Intent(context, QuickSpesaActivity::class.java).apply {
            putExtra("gruppoId", data.gruppoId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    } else null
    val btnAction = if (openIntent != null) actionStartActivity(openIntent)
                    else actionStartActivity<MainActivity>()

    // Outer Column: header + totals (fixed) | categories (flexible) | button (fixed)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surface)
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier          = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = "NoteSpese",
                style    = TextStyle(fontSize = 11.sp, color = ColorProvider(primary), fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (data.loaded) {
                Text(text = data.mese, style = TextStyle(fontSize = 11.sp, color = ColorProvider(hint)))
            }
        }

        if (!data.loaded) {
            Spacer(GlanceModifier.height(10.dp))
            Text(
                text  = "Apri l'app per aggiornare il widget",
                style = TextStyle(fontSize = 13.sp, color = ColorProvider(hint)),
            )
            return@Column
        }

        Text(
            text     = data.nomeGruppo,
            style    = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColorProvider(onSurface)),
            modifier = GlanceModifier.padding(top = 2.dp, bottom = 8.dp),
        )

        // ── Totals row ──────────────────────────────────────────────────────
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(text = "Spese", style = TextStyle(fontSize = 10.sp, color = ColorProvider(hint)))
                Text(
                    text  = fmt.format(data.totaleSpese),
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorProvider(rosso)),
                )
            }
            Box(modifier = GlanceModifier.width(1.dp).height(36.dp).background(divider)) {}
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(text = "Entrate", style = TextStyle(fontSize = 10.sp, color = ColorProvider(hint)))
                Text(
                    text  = fmt.format(data.totaleEntrate),
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorProvider(verde)),
                )
            }
        }

        Spacer(GlanceModifier.height(8.dp))
        Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(divider)) {}
        Spacer(GlanceModifier.height(6.dp))

        // ── Categorie (defaultWeight = fill remaining space above button) ────
        Column(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
            if (data.perCategoria.isEmpty()) {
                Text(
                    text  = "Nessuna spesa questo mese",
                    style = TextStyle(fontSize = 12.sp, color = ColorProvider(hint)),
                )
            } else {
                data.perCategoria.forEach { cat ->
                    RigaCategoria(cat = cat, fmt = fmt, hint = hint, rosso = rosso, onSurface = onSurface)
                    Spacer(GlanceModifier.height(5.dp))
                }
            }
        }

        Spacer(GlanceModifier.height(6.dp))

        // ── Aggiungi spesa button (always pinned at bottom) ──────────────────
        Box(
            modifier         = GlanceModifier
                .fillMaxWidth()
                .background(primary)
                .clickable(btnAction)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = "+",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColorProvider(onPrimary)),
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

@Composable
private fun RigaCategoria(
    cat: CategoriaWidget,
    fmt: NumberFormat,
    hint: Color,
    rosso: Color,
    onSurface: Color,
) {
    val colore = try {
        Color(android.graphics.Color.parseColor(cat.colore))
    } catch (_: Exception) { Color(0xFF9E9E9E) }

    val superaBudget = cat.budget > 0 && cat.totale > cat.budget

    Row(
        modifier          = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Color dot
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .background(if (superaBudget) rosso else colore),
        ) {}
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text     = cat.nome,
            style    = TextStyle(fontSize = 12.sp, color = ColorProvider(onSurface)),
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            text  = if (cat.budget > 0)
                "${fmt.format(cat.totale)} / ${fmt.format(cat.budget)}"
            else
                fmt.format(cat.totale),
            style = TextStyle(
                fontSize   = 11.sp,
                color      = ColorProvider(if (superaBudget) rosso else onSurface),
                fontWeight = if (superaBudget) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}
