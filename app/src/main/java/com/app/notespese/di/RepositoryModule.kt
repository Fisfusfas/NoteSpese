package com.app.notespese.di

import com.app.notespese.data.repository.BudgetRepository
import com.app.notespese.data.repository.CategoriaRepository
import com.app.notespese.data.repository.DebitoRepository
import com.app.notespese.data.repository.EntrataRepository
import com.app.notespese.data.repository.FirebaseBudgetRepository
import com.app.notespese.data.repository.FirebaseCategoriaRepository
import com.app.notespese.data.repository.FirebaseDebitoRepository
import com.app.notespese.data.repository.FirebaseEntrataRepository
import com.app.notespese.data.repository.FirebaseGruppoRepository
import com.app.notespese.data.repository.FirebaseInvitoRepository
import com.app.notespese.data.repository.FirebaseRicorrenzaRepository
import com.app.notespese.data.repository.FirebaseSaldoRepository
import com.app.notespese.data.repository.FirebaseSpesaRepository
import com.app.notespese.data.repository.GruppoRepository
import com.app.notespese.data.repository.InvitoRepository
import com.app.notespese.data.repository.RicorrenzaRepository
import com.app.notespese.data.repository.SaldoRepository
import com.app.notespese.data.repository.SpesaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindGruppoRepository(impl: FirebaseGruppoRepository): GruppoRepository

    @Binds @Singleton
    abstract fun bindSpesaRepository(impl: FirebaseSpesaRepository): SpesaRepository

    @Binds @Singleton
    abstract fun bindEntrataRepository(impl: FirebaseEntrataRepository): EntrataRepository

    @Binds @Singleton
    abstract fun bindCategoriaRepository(impl: FirebaseCategoriaRepository): CategoriaRepository

    @Binds @Singleton
    abstract fun bindSaldoRepository(impl: FirebaseSaldoRepository): SaldoRepository

    @Binds @Singleton
    abstract fun bindDebitoRepository(impl: FirebaseDebitoRepository): DebitoRepository

    @Binds @Singleton
    abstract fun bindInvitoRepository(impl: FirebaseInvitoRepository): InvitoRepository

    @Binds @Singleton
    abstract fun bindRicorrenzaRepository(impl: FirebaseRicorrenzaRepository): RicorrenzaRepository

    @Binds @Singleton
    abstract fun bindBudgetRepository(impl: FirebaseBudgetRepository): BudgetRepository
}
