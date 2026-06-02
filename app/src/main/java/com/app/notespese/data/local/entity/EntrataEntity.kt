package com.app.notespese.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.app.notespese.data.model.Entrata
import com.google.firebase.Timestamp
import java.util.Date

@Entity(tableName = "entrate")
data class EntrataEntity(
    @PrimaryKey val id: String,
    val gruppoId: String,
    val importo: Double,
    val persona: String,
    val categoriaId: String,
    val mese: Int,
    val anno: Int,
    val note: String,
    val dataMillis: Long?,
)

fun Entrata.toEntity(gruppoId: String): EntrataEntity = EntrataEntity(
    id = id,
    gruppoId = gruppoId,
    importo = importo,
    persona = persona,
    categoriaId = categoriaId,
    mese = mese,
    anno = anno,
    note = note,
    dataMillis = data?.toDate()?.time,
)

fun EntrataEntity.toModel(): Entrata = Entrata(
    id = id,
    importo = importo,
    persona = persona,
    categoriaId = categoriaId,
    mese = mese,
    anno = anno,
    note = note,
    data = dataMillis?.let { Timestamp(Date(it)) },
)
