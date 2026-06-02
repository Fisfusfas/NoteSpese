package com.app.notespese.data.local.entity

import androidx.room.Entity
import com.app.notespese.data.model.Membro
import com.google.firebase.Timestamp
import java.util.Date

@Entity(tableName = "membri", primaryKeys = ["gruppoId", "userId"])
data class MembroEntity(
    val gruppoId: String,
    val userId: String,
    val ruolo: String,
    val nominativoLocale: String,
    val aggiuntoIlMillis: Long?,
    val widgetDefault: Boolean,
)

fun Membro.toEntity(gruppoId: String): MembroEntity = MembroEntity(
    gruppoId = gruppoId,
    userId = userId.ifBlank { id },
    ruolo = ruolo,
    nominativoLocale = nominativoLocale,
    aggiuntoIlMillis = aggiuntoIl?.toDate()?.time,
    widgetDefault = widgetDefault,
)

fun MembroEntity.toModel(): Membro = Membro(
    id = userId,
    userId = userId,
    ruolo = ruolo,
    nominativoLocale = nominativoLocale,
    aggiuntoIl = aggiuntoIlMillis?.let { Timestamp(Date(it)) },
    widgetDefault = widgetDefault,
)
