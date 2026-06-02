package com.app.notespese.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.notespese.data.model.Categoria
import com.app.notespese.ui.gruppi.COLORI_GRUPPO
import com.app.notespese.ui.gruppi.parseColore

// ── Chip orizzontale (uso legacy / entrate) ───────────────────────────────────

@Composable
fun CategoriaSelector(
    categorie: List<Categoria>,
    categoriaSelezionataId: String,
    onSeleziona: (String) -> Unit,
    onCreaCategoria: (nome: String, colore: String, icona: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostraDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text     = "Categoria",
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(
            modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categorie.forEach { cat ->
                val selezionata = cat.id == categoriaSelezionataId
                FilterChip(
                    selected = selezionata,
                    onClick  = { onSeleziona(if (selezionata) "" else cat.id) },
                    label    = { Text(cat.nome) },
                    leadingIcon = {
                        if (selezionata) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                        } else {
                            Icon(iconaCategoria(cat.icona), contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize),
                                tint = parseColore(cat.colore))
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor  = parseColore(cat.colore).copy(alpha = 0.2f),
                        selectedLabelColor      = parseColore(cat.colore),
                        selectedLeadingIconColor = parseColore(cat.colore),
                    ),
                )
            }
            InputChip(
                selected    = false,
                onClick     = { mostraDialog = true },
                label       = { Text("Nuova") },
                leadingIcon = {
                    Icon(Icons.Default.Add, contentDescription = "Nuova categoria", modifier = Modifier.size(FilterChipDefaults.IconSize))
                },
            )
        }
    }

    if (mostraDialog) {
        DialogNuovaCategoria(
            onConferma = { nome, colore, icona ->
                onCreaCategoria(nome, colore, icona)
                mostraDialog = false
            },
            onDismiss = { mostraDialog = false },
        )
    }
}

// ── Griglia (form inserimento spesa) ─────────────────────────────────────────

@Composable
fun CategoriaGrid(
    categorie: List<Categoria>,
    categoriaSelezionataId: String,
    categoriaConsigliatId: String?,
    onSeleziona: (String) -> Unit,
    onCreaCategoria: (nome: String, colore: String, icona: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostraDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = "Categoria",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (categoriaConsigliatId != null) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Default.Star,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.tertiary,
                        modifier           = Modifier.size(12.dp),
                    )
                    Text(
                        text  = "Suggerita",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        val righe = (categorie.map<Categoria, Categoria?> { it } + listOf(null)).chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            righe.forEach { riga ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    riga.forEach { cat ->
                        if (cat != null) {
                            CategoriaCella(
                                categoria   = cat,
                                selezionata = cat.id == categoriaSelezionataId,
                                consigliata = cat.id == categoriaConsigliatId,
                                onClick     = { onSeleziona(if (cat.id == categoriaSelezionataId) "" else cat.id) },
                                modifier    = Modifier.weight(1f),
                            )
                        } else {
                            NuovaCategoriaCella(
                                onClick  = { mostraDialog = true },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    repeat(3 - riga.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }

    if (mostraDialog) {
        DialogNuovaCategoria(
            onConferma = { nome, colore, icona ->
                onCreaCategoria(nome, colore, icona)
                mostraDialog = false
            },
            onDismiss = { mostraDialog = false },
        )
    }
}

@Composable
private fun CategoriaCella(
    categoria: Categoria,
    selezionata: Boolean,
    consigliata: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colore = parseColore(categoria.colore)
    val shape  = RoundedCornerShape(12.dp)

    val containerColor = when {
        selezionata -> colore.copy(alpha = 0.22f)
        consigliata -> MaterialTheme.colorScheme.tertiaryContainer
        else        -> MaterialTheme.colorScheme.surfaceVariant
    }
    val iconTint = when {
        selezionata -> colore
        consigliata -> colore.copy(alpha = 0.85f)
        else        -> colore.copy(alpha = 0.75f)
    }
    val labelColor = when {
        selezionata -> colore
        consigliata -> MaterialTheme.colorScheme.onTertiaryContainer
        else        -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .height(76.dp)
            .clip(shape)
            .background(containerColor)
            .then(
                if (selezionata) Modifier.border(1.5.dp, colore, shape)
                else if (consigliata) Modifier.border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), shape)
                else Modifier
            )
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier              = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.Center,
        ) {
            Icon(
                imageVector        = iconaCategoria(categoria.icona),
                contentDescription = null,
                tint               = iconTint,
                modifier           = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text      = categoria.nome,
                style     = MaterialTheme.typography.labelSmall,
                color     = labelColor,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        // Badge suggerimento
        if (consigliata) {
            Icon(
                imageVector        = Icons.Default.Star,
                contentDescription = "Suggerita",
                tint               = MaterialTheme.colorScheme.tertiary,
                modifier           = Modifier
                    .size(14.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp),
            )
        }
    }
}

@Composable
private fun NuovaCategoriaCella(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = "Nuova categoria",
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Nuova",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ── Dialog creazione categoria ────────────────────────────────────────────────

@Composable
private fun DialogNuovaCategoria(
    onConferma: (nome: String, colore: String, icona: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nome              by rememberSaveable { mutableStateOf("") }
    var coloreSelezionato by remember { mutableStateOf(COLORI_GRUPPO.first()) }
    var iconaSelezionata  by remember { mutableStateOf("label") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Nuova categoria") },
        text             = {
            Column {
                OutlinedTextField(
                    value           = nome,
                    onValueChange   = { nome = it },
                    label           = { Text("Nome") },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier        = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("Colore", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    COLORI_GRUPPO.forEach { hex ->
                        val colore = parseColore(hex)
                        FilterChip(
                            selected = hex == coloreSelezionato,
                            onClick  = { coloreSelezionato = hex },
                            label    = { Text("") },
                            leadingIcon = {
                                if (hex == coloreSelezionato)
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor         = colore.copy(alpha = 0.15f),
                                selectedContainerColor = colore.copy(alpha = 0.4f),
                            ),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Icona", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ICONE_CATEGORIA.forEach { (key, icon) ->
                        FilterChip(
                            selected = key == iconaSelezionata,
                            onClick  = { iconaSelezionata = key },
                            label    = { Text("") },
                            leadingIcon = {
                                if (key == iconaSelezionata)
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                                else
                                    Icon(icon, null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                            },
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { if (nome.isNotBlank()) onConferma(nome, coloreSelezionato, iconaSelezionata) },
                enabled  = nome.isNotBlank(),
            ) { Text("Crea") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}
