package hopper.pfm.insect.core.domain.notification

import hopper.pfm.insect.core.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    /** All alerts known on this device, newest first. */
    fun observeAll(): Flow<List<AppNotification>>

    /** Number of unread alerts, for the badge of the bell. */
    fun observeUnreadCount(): Flow<Int>

    /** Marks one alert read on this device at once and sends the read state to the server in the background. */
    suspend fun markRead(id: String)

    suspend fun markAllRead()

    /** Unread alerts created after the given server timestamp, oldest first. */
    suspend fun unreadAfter(createdAt: Long): List<AppNotification>
}
