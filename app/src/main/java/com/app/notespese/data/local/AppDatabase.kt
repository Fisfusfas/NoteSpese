package com.app.notespese.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.notespese.data.local.dao.CategoriaDao
import com.app.notespese.data.local.dao.EntrataDao
import com.app.notespese.data.local.dao.GruppoDao
import com.app.notespese.data.local.dao.MembroDao
import com.app.notespese.data.local.dao.SpesaDao
import com.app.notespese.data.local.entity.CategoriaEntity
import com.app.notespese.data.local.entity.EntrataEntity
import com.app.notespese.data.local.entity.GruppoEntity
import com.app.notespese.data.local.entity.MembroEntity
import com.app.notespese.data.local.entity.SpesaEntity

@Database(
    entities = [
        SpesaEntity::class,
        EntrataEntity::class,
        CategoriaEntity::class,
        GruppoEntity::class,
        MembroEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spesaDao(): SpesaDao
    abstract fun entrataDao(): EntrataDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun gruppoDao(): GruppoDao
    abstract fun membroDao(): MembroDao
}
