package hopper.pfm.insect.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hopper.pfm.insect.core.database.PfmDatabase
import hopper.pfm.insect.core.domain.sync.SyncOverview
import hopper.pfm.insect.core.domain.sync.SyncRepository
import hopper.pfm.insect.core.domain.sync.SyncResult
import hopper.pfm.insect.core.model.Habit
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

@RunWith(AndroidJUnit4::class)
class HabitRepositoryTest {

    private class FakeSync : SyncRepository {
        var requests = 0
        override val overview: Flow<SyncOverview> = flowOf(SyncOverview())
        override fun requestSync() { requests++ }
        override suspend fun syncNow() = SyncResult()
    }

    private lateinit var db: PfmDatabase
    private lateinit var sync: FakeSync
    private lateinit var repo: HabitRepositoryImpl

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PfmDatabase::class.java).build()
        sync = FakeSync()
        repo = HabitRepositoryImpl(db.habitDao(), db, sync)
    }

    @After fun tearDown() { db.close() }

    private fun habit(id: String, title: String = "h$id", order: Int = -1, days: Set<Int> = setOf(3, 1)) =
        Habit(id, title, null, 30_000, null, days, null, order)

    private suspend fun ids() = repo.observeAll().first().map { it.id }

    @Test fun newHabitsGoToTheEndAndDaysAreStoredSorted() = runBlocking {
        repo.save(habit("a"))
        repo.save(habit("b"))
        repo.save(habit("c"))
        assertEquals(listOf("a", "b", "c"), ids())
        assertEquals(setOf(1, 3), repo.get("a")!!.daysOfWeek)
        assertEquals("1,3", db.habitDao().get("a")!!.daysOfWeek)
        assertTrue(db.habitDao().get("a")!!.dirty)
    }

    @Test fun editingKeepsThePlaceInTheList() = runBlocking {
        repo.save(habit("a"))
        repo.save(habit("b"))
        repo.save(habit("a", title = "renamed")) // sortOrder -1 = keep the existing place
        assertEquals(listOf("a", "b"), ids())
        assertEquals("renamed", repo.get("a")!!.title)
        assertEquals(2L, db.habitDao().get("a")!!.localRev)
    }

    @Test fun moveSwapsWithTheNeighbourAndMarksBothDirty() = runBlocking {
        repo.save(habit("a"))
        repo.save(habit("b"))
        repo.save(habit("c"))
        db.habitDao().dirtyMeta().forEach { db.habitDao().markPushed(it.id, 1, it.localRev) }
        repo.move("c", -1)
        assertEquals(listOf("a", "c", "b"), ids())
        assertEquals(setOf("b", "c"), db.habitDao().dirtyMeta().map { it.id }.toSet())
    }

    @Test fun movingTheFirstUpOrTheLastDownDoesNothing() = runBlocking {
        repo.save(habit("a"))
        repo.save(habit("b"))
        repo.move("a", -1)
        repo.move("b", 1)
        assertEquals(listOf("a", "b"), ids())
    }

    @Test fun deleteHidesTheHabitButKeepsTheTombstone() = runBlocking {
        repo.save(habit("a"))
        repo.delete("a")
        assertNull(repo.get("a"))
        assertTrue(ids().isEmpty())
        assertTrue(db.habitDao().get("a")!!.deleted)
    }
}
