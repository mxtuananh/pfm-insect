package hopper.pfm.insect.feature.notification

import android.content.res.Resources
import hopper.pfm.insect.core.common.Money
import hopper.pfm.insect.core.model.AlertKind
import hopper.pfm.insect.core.model.AppNotification
import hopper.pfm.insect.core.model.BudgetPeriod

/** The title and the second line of an alert, written in the language of the user from the structured fields. */
data class AlertText(val title: String, val body: String)

private fun periodWord(resources: Resources, period: BudgetPeriod): String = resources.getString(
    when (period) {
        BudgetPeriod.DAY -> R.string.notif_period_day
        BudgetPeriod.MONTH -> R.string.notif_period_month
        BudgetPeriod.YEAR -> R.string.notif_period_year
    },
)

/**
 * Used by the notification center and by the system notification of the background sync. [categoryName] is null for
 * an overall budget (or a category this device does not know).
 */
fun alertText(resources: Resources, alert: AppNotification, categoryName: String?, currency: String): AlertText {
    val name = categoryName ?: resources.getString(R.string.notif_all_expenses)
    val period = periodWord(resources, alert.period)
    val title = when (alert.kind) {
        AlertKind.THRESHOLD -> resources.getString(R.string.notif_title_threshold, name, alert.threshold ?: alert.percentUsed.toInt(), period)
        AlertKind.EXCEEDED -> resources.getString(R.string.notif_title_exceeded, name, period)
    }
    val body = resources.getString(
        R.string.notif_body,
        Money.formatWithSymbol(alert.spent, currency),
        Money.formatWithSymbol(alert.amount, currency),
        alert.percentUsed.toInt(),
    )
    return AlertText(title, body)
}

fun channelName(resources: Resources): String = resources.getString(R.string.notif_channel_name)

fun channelDescription(resources: Resources): String = resources.getString(R.string.notif_channel_description)

fun openActionLabel(resources: Resources): String = resources.getString(R.string.notif_action_open)
