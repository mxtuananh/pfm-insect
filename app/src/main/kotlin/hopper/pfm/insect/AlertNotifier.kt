package hopper.pfm.insect

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import hopper.pfm.insect.core.data.sync.SyncCompletionListener
import hopper.pfm.insect.core.datastore.AppPreferences
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.domain.expense.CategoryRepository
import hopper.pfm.insect.core.domain.notification.NotificationRepository
import hopper.pfm.insect.feature.notification.alertText
import hopper.pfm.insect.feature.notification.channelDescription
import hopper.pfm.insect.feature.notification.channelName
import hopper.pfm.insect.feature.notification.openActionLabel
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

const val ACTION_OPEN_NOTIFICATIONS = "hopper.pfm.insect.OPEN_NOTIFICATIONS"
private const val CHANNEL_ID = "budget_alerts"
private const val MAX_POSTED = 5

fun createAlertChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(CHANNEL_ID, channelName(context.resources), NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = channelDescription(context.resources)
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

/**
 * The server has no push, so this is the "app closed" path: after a background sync that found new alerts, a system
 * notification is posted for each (at most five), when the permission is granted. While the app is on screen the
 * notification center shows them, so nothing is posted.
 */
@Singleton
class AlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifications: NotificationRepository,
    private val categories: CategoryRepository,
    private val auth: AuthRepository,
    private val prefs: AppPreferences,
) : SyncCompletionListener {

    override suspend fun onSyncCompleted() {
        val last = prefs.lastNotifiedAlertAt()
        val foreground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        val fresh = notifications.unreadAfter(last)
        if (fresh.isEmpty()) return
        // The first run only remembers where we are: old alerts must not all pop up at once.
        val newest = fresh.maxOf { it.createdAt }
        if (last == 0L || foreground) {
            prefs.setLastNotifiedAlertAt(newest)
            return
        }
        if (!canPost()) {
            prefs.setLastNotifiedAlertAt(newest)
            return
        }
        val names = categories.observeAll().first().associate { it.id to it.name }
        val currency = auth.profile.first()?.currency ?: "VND"
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).setAction(ACTION_OPEN_NOTIFICATIONS).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        fresh.takeLast(MAX_POSTED).forEach { alert ->
            val text = alertText(context.resources, alert, alert.categoryId?.let(names::get), currency)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_alert)
                .setContentTitle(text.title)
                .setContentText(text.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
                .setContentIntent(open)
                .addAction(0, openActionLabel(context.resources), open)
                .setAutoCancel(true)
                .build()
            @Suppress("MissingPermission")
            NotificationManagerCompat.from(context).notify(alert.id.hashCode(), notification)
        }
        prefs.setLastNotifiedAlertAt(newest)
    }

    private fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NotifierModule {
    @Binds
    @IntoSet
    abstract fun bindAlertNotifier(impl: AlertNotifier): SyncCompletionListener
}
