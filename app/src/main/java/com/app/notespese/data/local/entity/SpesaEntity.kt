package com.app.notespese.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.app.notespese.data.model.Spesa
import com.google.firebase.Timestamp
import java.util.Date

@Entity(tableName = "spese")
data class SpesaEntity(
    @PrimaryKey val id: String,
    val gruppoId: String,
    val importo: Double,
    val descrizione: String,
    val categoriaId: String,
    val pagante: String,
    val condivisa: Boolean,
    val tipo: String,
    val dataMillis: Long?,
    val mese: Int,
    val anno: Int,
    val note: String,
)

fun Spesa.toEntity(gruppoId: String): SpesaEntity = SpesaEntity(
    id = id,
    gruppoId = gruppoId,
    importo = importo,
    descrizione = descrizione,
    categoriaId = categoriaId,
    pagante = pagante,
    condivisa = condivisa,
    tipo = tipo,
    dataMillis = data?.toDate()?.time,
    mese = mese,
    anno = anno,
    note = note,
)

fun SpesaEntity.toModel(): Spesa = Spesa(
    id = id,
    importo = importo,
    descrizione = descrizione,
    categoriaId = categoriaId,
    pagante = pagante,
    condivisa = condivisa,
    tipo = tipo,
    data = dataMillis?.let { Timestamp(Date(it)) },
    mese = mese,
    anno = anno,
    note = note,
)
