package com.app.notespese.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SelectoreMese(
    periodoLabel: String,
    onPrecedente: () -> Unit,
    onSuccessivo: () -> Unit,
    onTornaAdOggi: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier              = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrecedente) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Periodo precedente")
        }
        Text(
            text       = periodoLabel,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier   = if (onTornaAdOggi != null) Modifier.clickable(onClick = onTornaAdOggi) else Modifier,
        )
        IconButton(onClick = onSuccessivo) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Periodo successivo")
        }
    }
}

@Composable
fun StatoVuoto(testo: String, modifier: Modifier = Modifier) {
    Box(
        modifier         = modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text      = testo,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
