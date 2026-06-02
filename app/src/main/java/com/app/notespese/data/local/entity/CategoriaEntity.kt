package com.app.notespese.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.app.notespese.data.model.Categoria

@Entity(tableName = "categorie")
data class CategoriaEntity(
    @PrimaryKey val id: String,
    val gruppoId: String,
    val nome: String,
    val icona: String,
    val colore: String,
    val tipo: String,
)

fun Categoria.toEntity(gruppoId: String): CategoriaEntity = CategoriaEntity(
    id = id,
    gruppoId = gruppoId,
    nome = nome,
    icona = icona,
    colore = colore,
    tipo = tipo,
)

fun CategoriaEntity.toModel(): Categoria = Categoria(
    id = id,
    nome = nome,
    icona = icona,
    colore = colore,
    tipo = tipo,
)
