package hopper.pfm.insect.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category WHERE deleted = 0 ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun get(id: String): CategoryEntity?

    @Query("SELECT * FROM category WHERE dirty = 1")
    suspend fun dirty(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CategoryEntity>)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun remove(id: String)
}

@Dao
interface ExpenseDao {
    /** Both bounds inclusive, yyyy-MM-dd strings compare correctly as text. */
    @Query("SELECT * FROM expense WHERE deleted = 0 AND spentOn BETWEEN :from AND :to ORDER BY spentOn DESC, id")
    fun observeBetween(from: String, to: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense WHERE id = :id")
    suspend fun get(id: String): ExpenseEntity?

    @Query("SELECT * FROM expense WHERE dirty = 1")
    suspend fun dirty(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ExpenseEntity>)

    @Query("DELETE FROM expense WHERE id = :id")
    suspend fun remove(id: String)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit WHERE deleted = 0 ORDER BY sortOrder, title COLLATE NOCASE")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit WHERE id = :id")
    suspend fun get(id: String): HabitEntity?

    @Query("SELECT * FROM habit WHERE dirty = 1")
    suspend fun dirty(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<HabitEntity>)

    @Query("DELETE FROM habit WHERE id = :id")
    suspend fun remove(id: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget WHERE deleted = 0 ORDER BY startDate DESC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget WHERE id = :id")
    suspend fun get(id: String): BudgetEntity?

    @Query("SELECT * FROM budget WHERE dirty = 1")
    suspend fun dirty(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<BudgetEntity>)

    @Query("DELETE FROM budget WHERE id = :id")
    suspend fun remove(id: String)
}

@Dao
interface SyncCursorDao {
    @Query("SELECT cursor FROM sync_cursor WHERE resource = :resource")
    suspend fun get(resource: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: SyncCursorEntity)

    @Query("DELETE FROM sync_cursor")
    suspend fun clear()
}
