package hopper.pfm.insect.core.data

import hopper.pfm.insect.core.database.NotificationDao
import hopper.pfm.insect.core.database.NotificationEntity
import hopper.pfm.insect.core.domain.notification.NotificationRepository
import hopper.pfm.insect.core.domain.sync.SyncRepository
import hopper.pfm.insect.core.model.AlertKind
import hopper.pfm.insect.core.model.AppNotification
import hopper.pfm.insect.core.model.BudgetPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

internal fun NotificationEntity.toModel() = AppNotification(
    id = id,
    kind = if (kind == "exceeded") AlertKind.EXCEEDED else AlertKind.THRESHOLD,
    threshold = threshold,
    budgetId = budgetId,
    categoryId = categoryId,
    period = BudgetPeriod.entries.firstOrNull { it.name.equals(period, ignoreCase = true) } ?: BudgetPeriod.MONTH,
    periodStart = LocalDate.parse(periodStart),
    periodEnd = LocalDate.parse(periodEnd),
    amount = amount,
    spent = spent,
    percentUsed = percentUsed,
    createdAt = createdAt,
    read = read,
)

/** Reading is local first (works offline); the sync service sends the read state to the server afterwards. */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationDao,
    private val sync: SyncRepository,
) : NotificationRepository {

    override fun observeAll(): Flow<List<AppNotification>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    override fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

    override suspend fun markRead(id: String) {
        dao.markRead(id, System.currentTimeMillis())
        sync.requestSync()
    }

    override suspend fun markAllRead() {
        dao.markAllRead(System.currentTimeMillis())
        sync.requestSync()
    }

    override suspend fun unreadAfter(createdAt: Long): List<AppNotification> =
        dao.unreadAfter(createdAt).map { it.toModel() }
}
