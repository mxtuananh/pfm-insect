package hopper.pfm.insect.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A budget alert created by the server (the app never creates one). Only the read state can change on the device:
 * dirty means "marked read here, not yet sent" (PUT /v1/notifications/{id}/read). The sync columns follow the other tables.
 */
@Entity(tableName = "notification", indices = [Index("createdAt")])
data class NotificationEntity(
    @PrimaryKey val id: String,
    /** "threshold" or "exceeded". */
    val kind: String,
    val threshold: Int?,
    val budgetId: String,
    val categoryId: String?,
    /** "day", "month" or "year". */
    val period: String,
    val periodStart: String,
    val periodEnd: String,
    val amount: Long,
    val spent: Long,
    val percentUsed: Long,
    /** Epoch milliseconds, server clock. */
    val createdAt: Long,
    val read: Boolean,
    val readAt: Long?,
    val version: Long = 0,
    val deleted: Boolean = false,
    val dirty: Boolean = false,
    val localRev: Long = 0,
    val failure: String? = null,
)

@Dao
interface NotificationDao : SyncDao {
    @Query("SELECT id, version, deleted, dirty, localRev, failure FROM notification WHERE dirty = 1 AND failure IS NULL")
    override suspend fun dirtyMeta(): List<SyncMetaRow>

    @Query("SELECT id, version, deleted, dirty, localRev, failure FROM notification WHERE id = :id")
    override suspend fun meta(id: String): SyncMetaRow?

    @Query("UPDATE notification SET version = :version, dirty = CASE WHEN localRev = :rev THEN 0 ELSE 1 END WHERE id = :id")
    override suspend fun markPushed(id: String, version: Long, rev: Long)

    @Query("DELETE FROM notification WHERE id = :id AND deleted = 1 AND dirty = 0")
    override suspend fun removeIfDeletedAndClean(id: String)

    @Query("UPDATE notification SET failure = :code WHERE id = :id")
    override suspend fun markFailed(id: String, code: String)

    @Query("DELETE FROM notification WHERE dirty = 0")
    override suspend fun clearClean()

    @Query("SELECT COUNT(*) FROM notification WHERE dirty = 1 AND failure IS NULL")
    override fun observePending(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notification WHERE dirty = 1 AND failure IS NOT NULL")
    override fun observeRejected(): Flow<Int>

    @Query("SELECT * FROM notification ORDER BY createdAt DESC, id")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notification WHERE read = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT * FROM notification WHERE id = :id")
    suspend fun get(id: String): NotificationEntity?

    /** Unread alerts newer than a timestamp, oldest first (for the system notifications of a background sync). */
    @Query("SELECT * FROM notification WHERE read = 0 AND createdAt > :after ORDER BY createdAt")
    suspend fun unreadAfter(after: Long): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NotificationEntity)

    @Query("DELETE FROM notification WHERE id = :id")
    suspend fun remove(id: String)

    /** Marks one alert read on this device (a later push sends it to the server). */
    @Query("UPDATE notification SET read = 1, readAt = :now, dirty = 1, localRev = localRev + 1, failure = NULL WHERE id = :id AND read = 0")
    suspend fun markRead(id: String, now: Long)

    @Query("UPDATE notification SET read = 1, readAt = :now, dirty = 1, localRev = localRev + 1, failure = NULL WHERE read = 0")
    suspend fun markAllRead(now: Long)
}
