package com.app.notespese.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.app.notespese.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_BUDGET = "budget_alerts"
    }

    fun mostraBudgetSuperato(nomeCategoria: String, totale: Double, budget: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) return

        val fmt = NumberFormat.getCurrencyInstance(Locale.ITALY)
        val notif = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Budget superato: $nomeCategoria")
            .setContentText("${fmt.format(totale)} su ${fmt.format(budget)} previsti")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Hai superato il budget mensile per \"$nomeCategoria\".\n" +
                    "Speso: ${fmt.format(totale)}  •  Budget: ${fmt.format(budget)}"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(("budget_$nomeCategoria").hashCode(), notif)
    }
}
