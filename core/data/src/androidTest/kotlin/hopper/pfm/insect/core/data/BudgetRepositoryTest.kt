package hopper.pfm.insect.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hopper.pfm.insect.core.database.PfmDatabase
import hopper.pfm.insect.core.domain.sync.SyncOverview
import hopper.pfm.insect.core.domain.sync.SyncRepository
import hopper.pfm.insect.core.domain.sync.SyncResult
import hopper.pfm.insect.core.model.Budget
import hopper.pfm.insect.core.model.BudgetPeriod
import hopper.pfm.insect.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BudgetRepositoryTest {

    private class FakeSync : SyncRepository {
        var requests = 0
        override val overview: Flow<SyncOverview> = flowOf(SyncOverview())
        override fun requestSync() { requests++ }
        override suspend fun syncNow() = SyncResult()
    }

    private lateinit var db: PfmDatabase
    private lateinit var sync: FakeSync
    private lateinit var repo: BudgetRepositoryImpl

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PfmDatabase::class.java).build()
        sync = FakeSync()
        repo = BudgetRepositoryImpl(db.budgetDao(), db, sync)
    }

    @After fun tearDown() { db.close() }

    private fun budget(
        id: String = "b1",
        period: BudgetPeriod = BudgetPeriod.MONTH,
        recurring: Boolean = true,
        end: String? = "2027-01-31",
        thresholds: List<Int> = listOf(100, 80, 80),
    ) = Budget(id, null, period, 3_000_000, LocalDate.parse("2026-11-01"), end?.let(LocalDate::parse), recurring, thresholds)

    @Test fun periodIsStoredWithTheServerKeyAndReadBackAsTheEnum() = runBlocking {
        repo.save(budget(period = BudgetPeriod.YEAR))
        assertEquals("year", db.budgetDao().get("b1")!!.period)
        assertEquals(BudgetPeriod.YEAR, repo.get("b1")!!.period)
    }

    @Test fun thresholdsAreSortedWithoutDuplicates() = runBlocking {
        repo.save(budget())
        assertEquals("80,100", db.budgetDao().get("b1")!!.alertThresholds)
        assertEquals(listOf(80, 100), repo.get("b1")!!.alertThresholds)
    }

    @Test fun anEndDateIsDroppedForANonRepeatingBudget() = runBlocking {
        repo.save(budget(recurring = false, end = "2027-01-31"))
        assertNull(db.budgetDao().get("b1")!!.endDate)
    }

    @Test fun savingMakesItDirtyAndEveryEditRaisesLocalRev() = runBlocking {
        repo.save(budget())
        assertTrue(db.budgetDao().get("b1")!!.dirty)
        assertEquals(SyncStatus.PENDING, repo.get("b1")!!.syncStatus)
        db.budgetDao().markPushed("b1", 4, 1)
        repo.save(budget())
        assertEquals(2L, db.budgetDao().get("b1")!!.localRev)
        assertEquals(4L, db.budgetDao().get("b1")!!.version)
        assertEquals(2, sync.requests)
    }

    @Test fun deleteHidesItButKeepsTheRowForTheSync() = runBlocking {
        repo.save(budget())
        repo.delete("b1")
        assertNull(repo.get("b1"))
        assertTrue(repo.observeAll().first().isEmpty())
        assertTrue(db.budgetDao().get("b1")!!.deleted)
    }
}
