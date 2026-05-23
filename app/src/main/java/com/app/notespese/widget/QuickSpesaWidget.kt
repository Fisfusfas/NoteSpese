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
import com.app.notespese.ui.quick.QuickSpesaActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class QuickSpesaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val gruppoId   = fetchFirstGruppoId() ?: ""
        val nomeGruppo = fetchNomeGruppo(gruppoId)

        provideContent {
            GlanceTheme {
                QuickWidgetContent(
                    context    = context,
                    gruppoId   = gruppoId,
                    nomeGruppo = nomeGruppo,
                )
            }
        }
    }

    private suspend fun fetchFirstGruppoId(): String? {
        val userId = Firebase.auth.currentUser?.uid ?: return null
        return try {
            Firebase.firestore.collection("gruppi")
                .whereArrayContains("membroIds", userId)
                .limit(1)
                .get().await()
                .documents.firstOrNull()?.id
        } catch (e: Exception) { null }
    }

    private suspend fun fetchNomeGruppo(gruppoId: String): String {
        if (gruppoId.isEmpty()) return ""
        return try {
            Firebase.firestore.collection("gruppi").document(gruppoId)
                .get().await().getString("nome") ?: ""
        } catch (e: Exception) { "" }
    }
}

@Composable
private fun QuickWidgetContent(
    context: Context,
    gruppoId: String,
    nomeGruppo: String,
) {
    val surface   = Color(0xFFF5F5F5)
    val primary   = Color(0xFF1565C0)
    val onPrimary = Color.White
    val hint      = Color(0xFF757575)
    val fieldBg   = Color(0xFFE8EAF6)
    val fieldText = Color(0xFFBDBDBD)
    val border    = Color(0xFFBBDEFB)

    val openIntent = if (gruppoId.isNotEmpty()) {
        Intent(context, QuickSpesaActivity::class.java).apply {
            putExtra("gruppoId", gruppoId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    } else null

    val tapAction = if (openIntent != null) actionStartActivity(openIntent)
    else actionStartActivity<MainActivity>()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surface)
            .clickable(tapAction)
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
            if (nomeGruppo.isNotEmpty()) {
                Text(
                    text  = nomeGruppo,
                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(hint)),
                )
            }
        }

        Text(
            text     = "Aggiungi spesa",
            style    = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(top = 4.dp, bottom = 14.dp),
        )

        if (gruppoId.isEmpty()) {
            Text(
                text  = "Accedi all'app per configurare il widget",
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(hint)),
            )
            return@Column
        }

        // ── Fake importo field ───────────────────────────────────────────────
        FakeField(label = "Importo", placeholder = "€  0,00", primary = primary, fieldBg = fieldBg, border = border, fieldText = fieldText)

        Spacer(GlanceModifier.height(10.dp))

        // ── Fake descrizione field ───────────────────────────────────────────
        FakeField(label = "Descrizione", placeholder = "Cosa hai acquistato?", primary = primary, fieldBg = fieldBg, border = border, fieldText = fieldText)

        Spacer(GlanceModifier.height(10.dp))

        // ── Fake categoria field ─────────────────────────────────────────────
        FakeField(label = "Categoria", placeholder = "Seleziona categoria  ▾", primary = primary, fieldBg = fieldBg, border = border, fieldText = fieldText)

        Spacer(GlanceModifier.height(14.dp))

        // ── CTA button ──────────────────────────────────────────────────────
        Box(
            modifier         = GlanceModifier
                .fillMaxWidth()
                .background(primary)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = "+",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorProvider(onPrimary)),
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text  = "Tocca per aggiungere",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ColorProvider(onPrimary)),
                )
            }
        }
    }
}

@Composable
private fun FakeField(
    label: String,
    placeholder: String,
    primary: Color,
    fieldBg: Color,
    border: Color,
    fieldText: Color,
) {
    Column {
        Text(
            text     = label,
            style    = TextStyle(fontSize = 11.sp, color = ColorProvider(primary), fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.padding(bottom = 4.dp),
        )
        Box(
            modifier         = GlanceModifier
                .fillMaxWidth()
                .background(fieldBg)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text  = placeholder,
                style = TextStyle(fontSize = 14.sp, color = ColorProvider(fieldText)),
            )
        }
    }
}
