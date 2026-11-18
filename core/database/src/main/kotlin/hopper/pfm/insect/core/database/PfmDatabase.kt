package hopper.pfm.insect.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        HabitEntity::class,
        BudgetEntity::class,
        SyncCursorEntity::class,
        NotificationEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class PfmDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun habitDao(): HabitDao
    abstract fun budgetDao(): BudgetDao
    abstract fun syncCursorDao(): SyncCursorDao
    abstract fun notificationDao(): NotificationDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PfmDatabase =
        Room.databaseBuilder(context, PfmDatabase::class.java, "pfm.db").build()

    @Provides fun categoryDao(db: PfmDatabase) = db.categoryDao()
    @Provides fun expenseDao(db: PfmDatabase) = db.expenseDao()
    @Provides fun habitDao(db: PfmDatabase) = db.habitDao()
    @Provides fun budgetDao(db: PfmDatabase) = db.budgetDao()
    @Provides fun syncCursorDao(db: PfmDatabase) = db.syncCursorDao()
    @Provides fun notificationDao(db: PfmDatabase) = db.notificationDao()
}
