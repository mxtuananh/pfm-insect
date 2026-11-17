package hopper.pfm.insect.core.data.sync

import hopper.pfm.insect.core.data.SessionManager
import hopper.pfm.insect.core.database.BudgetDao
import hopper.pfm.insect.core.database.CategoryDao
import hopper.pfm.insect.core.database.ExpenseDao
import hopper.pfm.insect.core.database.HabitDao
import hopper.pfm.insect.core.database.PfmDatabase
import hopper.pfm.insect.core.database.SyncCursorDao
import hopper.pfm.insect.core.domain.sync.ResourceSyncer
import hopper.pfm.insect.core.domain.sync.SyncEngine
import hopper.pfm.insect.core.domain.sync.SyncOverview
import hopper.pfm.insect.core.domain.sync.SyncProblem
import hopper.pfm.insect.core.domain.sync.SyncRepository
import hopper.pfm.insect.core.domain.sync.SyncResult
import hopper.pfm.insect.core.model.SessionState
import hopper.pfm.insect.core.network.BudgetApi
import hopper.pfm.insect.core.network.CategoryApi
import hopper.pfm.insect.core.network.ExpenseApi
import hopper.pfm.insect.core.network.HabitApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs the sync engine, one round at a time, and publishes what the indicators need. Background scheduling (network
 * constraint, back-off, periodic runs) is done by WorkManager through SyncScheduler; this class does the work.
 */
@Singleton
class SyncManager @Inject constructor(
    categoryDao: CategoryDao,
    expenseDao: ExpenseDao,
    habitDao: HabitDao,
    budgetDao: BudgetDao,
    cursors: SyncCursorDao,
    db: PfmDatabase,
    categoryApi: CategoryApi,
    expenseApi: ExpenseApi,
    habitApi: HabitApi,
    budgetApi: BudgetApi,
    private val session: SessionManager,
    private val scheduler: SyncScheduler,
) : SyncRepository {

    // Categories first: expenses and budgets may refer to categories the server does not know yet.
    private val engine = SyncEngine(
        listOf(
            ResourceSyncer(CategorySyncAdapter(categoryDao, categoryApi, cursors, db)),
            ResourceSyncer(ExpenseSyncAdapter(expenseDao, expenseApi, cursors, db)),
            ResourceSyncer(HabitSyncAdapter(habitDao, habitApi, cursors, db)),
            ResourceSyncer(BudgetSyncAdapter(budgetDao, budgetApi, cursors, db)),
        ),
    )

    private val mutex = Mutex()
    private val running = MutableStateFlow(false)
    private val lastProblem = MutableStateFlow<SyncProblem?>(null)

    override val overview: Flow<SyncOverview> = combine(
        running,
        lastProblem,
        combine(categoryDao.observePending(), expenseDao.observePending(), habitDao.observePending(), budgetDao.observePending()) { a, b, c, d -> a + b + c + d },
        combine(categoryDao.observeRejected(), expenseDao.observeRejected(), habitDao.observeRejected(), budgetDao.observeRejected()) { a, b, c, d -> a + b + c + d },
    ) { syncing, problem, pending, rejected -> SyncOverview(syncing, pending, rejected, problem) }

    override fun requestSync() = scheduler.requestSync()

    override suspend fun syncNow(): SyncResult = mutex.withLock {
        if (session.state.value != SessionState.SignedIn) return SyncResult(problem = SyncProblem.AuthRequired)
        running.value = true
        try {
            var total = SyncResult()
            // Edits made while a round runs are picked up by one more round (bounded, so a busy user cannot loop forever).
            for (round in 1..MAX_ROUNDS) {
                val result = engine.run()
                total = SyncResult(total.pushed + result.pushed, total.rejected + result.rejected, total.applied + result.applied, result.problem)
                if (result.problem != null || result.pushed == 0) break
            }
            lastProblem.update { total.problem }
            total
        } finally {
            running.value = false
        }
    }

    private companion object {
        const val MAX_ROUNDS = 3
    }
}
