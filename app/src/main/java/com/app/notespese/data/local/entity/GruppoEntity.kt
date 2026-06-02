package com.app.notespese.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.app.notespese.data.model.Gruppo

@Entity(tableName = "gruppi")
data class GruppoEntity(
    @PrimaryKey val id: String,
    val nome: String,
    val descrizione: String,
    val creatoDa: String,
    val icona: String,
    val colore: String,
    val modalitaSplitDefault: String,
    // Lista di userId serializzata con separatore "|" — non usare TypeConverter automatico,
    // il mapper gestisce manualmente split/join per poter filtrare lato SQL con LIKE.
    val membroIdsStr: String,
    val giornoInizioMese: Int,
)

fun Gruppo.toEntity(): GruppoEntity = GruppoEntity(
    id = id,
    nome = nome,
    descrizione = descrizione,
    creatoDa = creatoDa,
    icona = icona,
    colore = colore,
    modalitaSplitDefault = modalitaSplitDefault,
    membroIdsStr = membroIds.joinToString("|"),
    giornoInizioMese = giornoInizioMese,
)

fun GruppoEntity.toModel(): Gruppo = Gruppo(
    id = id,
    nome = nome,
    descrizione = descrizione,
    creatoDa = creatoDa,
    icona = icona,
    colore = colore,
    modalitaSplitDefault = modalitaSplitDefault,
    membroIds = membroIdsStr.split("|").filter { it.isNotBlank() },
    giornoInizioMese = giornoInizioMese,
)
