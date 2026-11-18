package hopper.pfm.insect.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hopper.pfm.insect.core.common.TokenStore
import hopper.pfm.insect.core.datastore.KeystoreTokenStore
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.domain.HealthRepository
import hopper.pfm.insect.core.domain.expense.CategoryRepository
import hopper.pfm.insect.core.domain.expense.ExpenseRepository
import hopper.pfm.insect.core.domain.expense.UserSettings
import hopper.pfm.insect.core.domain.habit.HabitRepository
import hopper.pfm.insect.core.data.sync.SyncManager
import hopper.pfm.insect.core.domain.sync.SyncRepository
import hopper.pfm.insect.core.network.TokenProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindHealthRepository(impl: HealthRepositoryImpl): HealthRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindTokenStore(impl: KeystoreTokenStore): TokenStore

    @Binds
    abstract fun bindTokenProvider(impl: SessionManager): TokenProvider

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncManager): SyncRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindUserSettings(impl: UserSettingsImpl): UserSettings

    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository
}
