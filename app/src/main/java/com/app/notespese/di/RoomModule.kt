package com.app.notespese.di

import android.content.Context
import androidx.room.Room
import com.app.notespese.data.local.AppDatabase
import com.app.notespese.data.local.dao.CategoriaDao
import com.app.notespese.data.local.dao.EntrataDao
import com.app.notespese.data.local.dao.GruppoDao
import com.app.notespese.data.local.dao.MembroDao
import com.app.notespese.data.local.dao.SpesaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "notespese.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSpesaDao(db: AppDatabase): SpesaDao = db.spesaDao()

    @Provides
    fun provideEntrataDao(db: AppDatabase): EntrataDao = db.entrataDao()

    @Provides
    fun provideCategoriaDao(db: AppDatabase): CategoriaDao = db.categoriaDao()

    @Provides
    fun provideGruppoDao(db: AppDatabase): GruppoDao = db.gruppoDao()

    @Provides
    fun provideMembroDao(db: AppDatabase): MembroDao = db.membroDao()
}
