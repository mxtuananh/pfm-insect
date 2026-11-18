package hopper.pfm.insect.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.domain.budget.BudgetProgress
import hopper.pfm.insect.core.domain.budget.BudgetRepository
import hopper.pfm.insect.core.domain.budget.BudgetStatus
import hopper.pfm.insect.core.domain.budget.computeProgress
import hopper.pfm.insect.core.domain.expense.CategoryRepository
import hopper.pfm.insect.core.domain.expense.ExpenseRepository
import hopper.pfm.insect.core.domain.habit.HabitRepository
import hopper.pfm.insect.core.domain.notification.NotificationRepository
import hopper.pfm.insect.core.domain.sync.SyncProblem
import hopper.pfm.insect.core.domain.sync.SyncRepository
import hopper.pfm.insect.core.model.AppNotification
import hopper.pfm.insect.core.model.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AlertDay(val date: LocalDate, val alerts: List<AppNotification>)

data class NotificationCenterUiState(
    val loading: Boolean = true,
    /** Budgets that are close to or over their limit right now, computed from the local database. */
    val warnings: List<BudgetProgress> = emptyList(),
    /** Alerts of the server, newest day first. */
    val days: List<AlertDay> = emptyList(),
    val unread: Int = 0,
    val categories: Map<String, Category> = emptyMap(),
    val currency: String = "VND",
    val offline: Boolean = false,
    val today: LocalDate = LocalDate.now(),
) {
    val isEmpty: Boolean get() = warnings.isEmpty() && days.isEmpty()
}

@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val notifications: NotificationRepository,
    budgets: BudgetRepository,
    expenses: ExpenseRepository,
    habits: HabitRepository,
    categories: CategoryRepository,
    auth: AuthRepository,
    sync: SyncRepository,
) : ViewModel() {

    val state: StateFlow<NotificationCenterUiState> = combine(
        notifications.observeAll(),
        budgets.observeAll(),
        expenses.observeBetween(LocalDate.now().withDayOfYear(1), LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear())),
        habits.observeAll(),
        categories.observeAll(),
    ) { alerts, allBudgets, yearExpenses, allHabits, cats ->
        Snapshot(alerts, allBudgets, yearExpenses, allHabits, cats)
    }.combine(auth.profile) { snapshot, profile -> snapshot to profile }
        .combine(sync.overview) { (snapshot, profile), overview ->
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val warnings = computeProgress(snapshot.budgets, snapshot.expenses, snapshot.habits, today)
                .filter { it.status != BudgetStatus.OK }
                .sortedByDescending { it.status.ordinal }
            NotificationCenterUiState(
                loading = false,
                warnings = warnings,
                days = snapshot.alerts
                    .groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
                    .entries.sortedByDescending { it.key }
                    .map { (date, list) -> AlertDay(date, list.sortedByDescending { it.createdAt }) },
                unread = snapshot.alerts.count { !it.read },
                categories = snapshot.categories.associateBy { it.id },
                currency = profile?.currency ?: "VND",
                offline = overview.problem == SyncProblem.Retry,
                today = today,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationCenterUiState())

    fun markRead(id: String) {
        viewModelScope.launch { notifications.markRead(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { notifications.markAllRead() }
    }

    private class Snapshot(
        val alerts: List<AppNotification>,
        val budgets: List<hopper.pfm.insect.core.model.Budget>,
        val expenses: List<hopper.pfm.insect.core.model.Expense>,
        val habits: List<hopper.pfm.insect.core.model.Habit>,
        val categories: List<Category>,
    )
}

/** The unread count of the bell in the top bar. */
@HiltViewModel
class UnreadBadgeViewModel @Inject constructor(notifications: NotificationRepository) : ViewModel() {
    val unread: StateFlow<Int> = notifications.observeUnreadCount()
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
