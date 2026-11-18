package hopper.pfm.insect.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao : SyncDao {
    @Query("SELECT id, version, deleted, dirty, localRev, failure FROM category WHERE dirty = 1 AND failure IS NULL")
    override suspend fun dirtyMeta(): List<SyncMetaRow>

    @Query("SELECT id, version, deleted, dirty, localRev, failure FROM category WHERE id = :id")
    override suspend fun meta(id: String): SyncMetaRow?

    @Query("UPDATE category SET version = :version, dirty = CASE WHEN localRev = :rev THEN 0 ELSE 1 END WHERE id = :id")
    override suspend fun markPushed(id: String, version: Long, rev: Long)

    @Query("DELETE FROM category WHERE id = :id AND deleted = 1 AND dirty = 0")
    override suspend fun removeIfDeletedAndClean(id: String)

    @Query("UPDATE category SET failure = :code WHERE id = :id")
    override suspend fun markFailed(id: String, code: String)

    @Query("DELETE FROM category WHERE dirty = 0")
    override suspend fun clearClean()

    @Query("SELECT COUNT(*) FROM category WHERE dirty = 1 AND failure IS NULL")
    override fun observePending(): Flow<Int>

    @Query("SELECT COUNT(*) FROM category WHERE dirty = 1 AND failure IS NOT NULL")
    override fun observeRejected(): Flow<Int>

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
interface ExpenseDao : SyncDao {
    @Query("SELECT id, version, deleted, dirty, localRev, failure FROM expense WHERE dirty = 1 AND failure IS NULL")
    override suspend fun dirtyMeta(): List<SyncMetaRow>

    @Query("SELECT id, version, deleted, dirty, localRev, failure FROM expense WHERE id = :id")
    override suspend fun meta(id: String): SyncMetaRow?

    @Query("UPDATE expense SET version = :version, dirty = CASE WHEN localRev = :rev THEN 0 ELSE 1 END WHERE id = :id")
    override suspend fun markPushed(id: String, version: Long, rev: Long)

    @Query("DELETE FROM expense WHERE id = :id AND deleted = 1 AND dirty = 0")
    override suspend fun removeIfDeletedAndClean(id: String)

    @Query("UPDATE expense SET failure = :code WHERE id = :id")
    override suspend fun markFailed(id: String, code: String)

    @Query("DELETE FROM expense WHERE dirty = 0")
    override suspend fun clearClean()

    @Query("SELECT COUNT(*) FROM expense WHERE dirty = 1 AND failure IS NULL")
    override fun observePending(): Flow<Int>

    @Query("SELECT COUNT(*) FROM expense WHERE dirty = 1 AND failure IS NOT NULL")
    override fun observeRejected(): Flow<Int>

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
interface HabitDao : SyncDao {
    @Query("SELECT * FROM habit WHERE deleted = 0 ORDER BY sortOrder, title COLLATE NOCASE")
    suspend fun listLive(): List<HabitEntity>

    @Query("SELECT id, version, deleted, dirty, localRev, failure FROM habit WHERE dirty = 1 AND failure IS NULL")
    override suspend fun dirtyMeta(): List<SyncMetaRow>

    @Query("SELECT id, version, deleted, dirty, localRev, failure FROM habit WHERE id = :id")
    override suspend fun meta(id: String): SyncMetaRow?

    @Query("UPDATE habit SET version = :version, dirty = CASE WHEN localRev = :rev THEN 0 ELSE 1 END WHERE id = :id")
    override suspend fun markPushed(id: String, version: Long, rev: Long)

    @Query("DELETE FROM habit WHERE id = :id AND deleted = 1 AND dirty = 0")
    override suspend fun removeIfDeletedAndClean(id: String)

    @Query("UPDATE habit SET failure = :code WHERE id = :id")
    override suspend fun markFailed(id: String, code: String)

    @Query("DELETE FROM habit WHERE dirty = 0")
    override suspend fun clearClean()

    @Query("SELECT COUNT(*) FROM habit WHERE dirty = 1 AND failure IS NULL")
    override fun observePending(): Flow<Int>

    @Query("SELECT COUNT(*) FROM habit WHERE dirty = 1 AND failure IS NOT NULL")
    override fun observeRejected(): Flow<Int>

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
interface BudgetDao : SyncDao {
    @Query("SELECT id, version, deleted, dirty, localRev, failure FROM budget WHERE dirty = 1 AND failure IS NULL")
    override suspend fun dirtyMeta(): List<SyncMetaRow>

    @Query("SELECT id, version, deleted, dirty, localRev, failure FROM budget WHERE id = :id")
    override suspend fun meta(id: String): SyncMetaRow?

    @Query("UPDATE budget SET version = :version, dirty = CASE WHEN localRev = :rev THEN 0 ELSE 1 END WHERE id = :id")
    override suspend fun markPushed(id: String, version: Long, rev: Long)

    @Query("DELETE FROM budget WHERE id = :id AND deleted = 1 AND dirty = 0")
    override suspend fun removeIfDeletedAndClean(id: String)

    @Query("UPDATE budget SET failure = :code WHERE id = :id")
    override suspend fun markFailed(id: String, code: String)

    @Query("DELETE FROM budget WHERE dirty = 0")
    override suspend fun clearClean()

    @Query("SELECT COUNT(*) FROM budget WHERE dirty = 1 AND failure IS NULL")
    override fun observePending(): Flow<Int>

    @Query("SELECT COUNT(*) FROM budget WHERE dirty = 1 AND failure IS NOT NULL")
    override fun observeRejected(): Flow<Int>

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

/** The sync columns of a record, without its own fields. */
data class SyncMetaRow(
    val id: String,
    val version: Long,
    val deleted: Boolean,
    val dirty: Boolean,
    val localRev: Long,
    val failure: String?,
)

/** Operations every synchronized table offers to the sync engine. */
interface SyncDao {
    /** Dirty rows that were not permanently rejected before. */
    suspend fun dirtyMeta(): List<SyncMetaRow>

    suspend fun meta(id: String): SyncMetaRow?

    /** Stores the version; dirty is cleared only when localRev is still rev (no edit in between). */
    suspend fun markPushed(id: String, version: Long, rev: Long)

    suspend fun removeIfDeletedAndClean(id: String)

    suspend fun markFailed(id: String, code: String)

    suspend fun clearClean()

    fun observePending(): Flow<Int>

    fun observeRejected(): Flow<Int>
}
