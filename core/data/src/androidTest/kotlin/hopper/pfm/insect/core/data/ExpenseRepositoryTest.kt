package hopper.pfm.insect.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hopper.pfm.insect.core.database.PfmDatabase
import hopper.pfm.insect.core.domain.sync.SyncOverview
import hopper.pfm.insect.core.domain.sync.SyncRepository
import hopper.pfm.insect.core.domain.sync.SyncResult
import hopper.pfm.insect.core.model.Expense
import hopper.pfm.insect.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ExpenseRepositoryTest {

    private class FakeSync : SyncRepository {
        var requests = 0
        override val overview: Flow<SyncOverview> = flowOf(SyncOverview())
        override fun requestSync() { requests++ }
        override suspend fun syncNow() = SyncResult()
    }

    private lateinit var db: PfmDatabase
    private lateinit var sync: FakeSync
    private lateinit var repo: ExpenseRepositoryImpl

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PfmDatabase::class.java).build()
        sync = FakeSync()
        repo = ExpenseRepositoryImpl(db.expenseDao(), db, sync)
    }

    @After fun tearDown() { db.close() }

    private fun expense(id: String = "e1", title: String = "Pho", amount: Long = 45_000, date: String = "2026-11-17") =
        Expense(id, title, "  note  ", amount, LocalDate.parse(date), null)

    @Test fun savingMakesTheRecordDirtyAndAsksForASync() = runBlocking {
        repo.save(expense())
        val row = db.expenseDao().get("e1")!!
        assertTrue(row.dirty)
        assertEquals(1L, row.localRev)
        assertEquals(0L, row.version)
        assertEquals("note", row.note)
        assertEquals(1, sync.requests)
        assertEquals(SyncStatus.PENDING, repo.get("e1")!!.syncStatus)
    }

    @Test fun everyEditRaisesLocalRevAndKeepsTheServerVersion() = runBlocking {
        repo.save(expense())
        db.expenseDao().markPushed("e1", 3, 1) // the server accepted revision 1
        repo.save(expense(title = "Pho dac biet"))
        val row = db.expenseDao().get("e1")!!
        assertEquals(2L, row.localRev)
        assertEquals(3L, row.version)
        assertTrue(row.dirty)
    }

    @Test fun editingARejectedRecordClearsTheFailure() = runBlocking {
        repo.save(expense())
        db.expenseDao().markFailed("e1", "validation_failed")
        assertEquals(SyncStatus.FAILED, repo.get("e1")!!.syncStatus)
        repo.save(expense(amount = 50_000))
        assertNull(db.expenseDao().get("e1")!!.failure)
        assertEquals(SyncStatus.PENDING, repo.get("e1")!!.syncStatus)
    }

    @Test fun deleteHidesTheRecordButKeepsTheRowForTheSync() = runBlocking {
        repo.save(expense())
        db.expenseDao().markPushed("e1", 1, 1)
        repo.delete("e1")
        assertNull(repo.get("e1"))
        val row = db.expenseDao().get("e1")!!
        assertTrue(row.deleted && row.dirty)
        assertTrue(repo.observeBetween(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30")).first().isEmpty())
    }

    @Test fun savingAgainAfterADeleteRestoresIt() = runBlocking {
        repo.save(expense())
        repo.delete("e1")
        repo.save(expense())
        assertFalse(db.expenseDao().get("e1")!!.deleted)
        assertEquals("Pho", repo.get("e1")!!.title)
    }

    @Test fun rangeQueryIsInclusiveAndOrderedNewestFirst() = runBlocking {
        repo.save(expense("a", date = "2026-11-01"))
        repo.save(expense("b", date = "2026-11-30"))
        repo.save(expense("c", date = "2026-12-01"))
        val items = repo.observeBetween(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30")).first()
        assertEquals(listOf("b", "a"), items.map { it.id })
    }

}
