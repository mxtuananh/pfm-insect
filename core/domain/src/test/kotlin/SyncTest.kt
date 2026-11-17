import hopper.pfm.insect.core.common.AppError
import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.domain.sync.ChangesPage
import hopper.pfm.insect.core.domain.sync.InitialLoad
import hopper.pfm.insect.core.domain.sync.LocalMeta
import hopper.pfm.insect.core.domain.sync.RemoteItem
import hopper.pfm.insect.core.domain.sync.ResourceSyncer
import hopper.pfm.insect.core.domain.sync.SyncAdapter
import hopper.pfm.insect.core.domain.sync.SyncEngine
import hopper.pfm.insect.core.domain.sync.SyncProblem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory resource whose "entity" is just a string; the fake follows the same rules as the Room implementation. */
private class FakeAdapter(override val name: String, val log: MutableList<String> = mutableListOf()) : SyncAdapter<String> {
    val rows = linkedMapOf<String, LocalMeta>()
    val values = mutableMapOf<String, String>()
    var cursor: String? = null
    var pushResults = ArrayDeque<Outcome<Long>>()
    var defaultPush: (LocalMeta) -> Outcome<Long> = { Outcome.Success(it.version + 1) }
    var changes = ArrayDeque<Outcome<ChangesPage<String>>>()
    var initial: Outcome<InitialLoad<String>> = Outcome.Success(InitialLoad(emptyList(), "c0"))
    var initialCalls = 0

    fun local(id: String, version: Long = 0, dirty: Boolean = false, deleted: Boolean = false, rev: Long = 1, value: String = "v") {
        rows[id] = LocalMeta(id, version, deleted, dirty, rev, null)
        values[id] = value
    }

    override suspend fun dirtyRecords() = rows.values.filter { it.dirty && it.failure == null }
    override suspend fun meta(id: String) = rows[id]
    override suspend fun push(meta: LocalMeta): Outcome<Long> {
        log += "$name:push:${meta.id}"
        return pushResults.removeFirstOrNull() ?: defaultPush(meta)
    }

    override suspend fun markPushed(id: String, version: Long, rev: Long) {
        val m = rows[id] ?: return
        val dirty = m.localRev != rev
        rows[id] = m.copy(version = version, dirty = dirty)
        if (m.deleted && !dirty) rows.remove(id)
    }

    override suspend fun markFailed(id: String, code: String) {
        rows[id] = rows.getValue(id).copy(failure = code)
    }

    override suspend fun removeLocal(id: String) { rows.remove(id); values.remove(id) }
    override suspend fun upsertRemote(entity: String) {
        // the entity string is "id:version:value"
        val (id, version, value) = entity.split(":")
        rows[id] = LocalMeta(id, version.toLong(), false, false, 0, null)
        values[id] = value
    }

    override suspend fun loadInitial(): Outcome<InitialLoad<String>> { initialCalls++; return initial }
    override suspend fun fetchChanges(cursor: String) = changes.removeFirst()
    override suspend fun cursor() = cursor
    override suspend fun saveCursor(cursor: String) { this.cursor = cursor }
    override suspend fun clearNonDirty() { rows.entries.removeAll { !it.value.dirty }; }
    override suspend fun <R> transaction(block: suspend () -> R) = block()
}

private fun item(id: String, version: Long, deleted: Boolean = false, value: String = "remote") =
    RemoteItem(id, version, deleted, "$id:$version:$value")

private fun page(vararg items: RemoteItem<String>, next: String = "n", more: Boolean = false) =
    Outcome.Success(ChangesPage(items.toList(), next, more))

class SyncTest {
    @Test fun pushMarksCleanAndStoresVersion() = runTest {
        val a = FakeAdapter("expenses").apply { local("e1", dirty = true) }
        val report = ResourceSyncer(a).push()
        assertEquals(1, report.pushed)
        assertEquals(false, a.rows.getValue("e1").dirty)
        assertEquals(1L, a.rows.getValue("e1").version)
    }

    @Test fun editDuringFlightStaysDirty() = runTest {
        val a = FakeAdapter("expenses").apply { local("e1", dirty = true, rev = 1) }
        a.defaultPush = { meta ->
            a.rows[meta.id] = a.rows.getValue(meta.id).copy(localRev = 2) // the user edits while the PUT is in flight
            Outcome.Success(5)
        }
        ResourceSyncer(a).push()
        assertTrue(a.rows.getValue("e1").dirty)
        assertEquals(5L, a.rows.getValue("e1").version)
    }

    @Test fun createdAndDeletedOfflineIsNeverSent() = runTest {
        val a = FakeAdapter("expenses").apply { local("e1", version = 0, dirty = true, deleted = true) }
        ResourceSyncer(a).push()
        assertTrue(a.log.isEmpty())
        assertNull(a.rows["e1"])
    }

    @Test fun syncedDeleteIsSentThenRemoved() = runTest {
        val a = FakeAdapter("expenses").apply { local("e1", version = 3, dirty = true, deleted = true) }
        ResourceSyncer(a).push()
        assertEquals(listOf("expenses:push:e1"), a.log)
        assertNull(a.rows["e1"])
    }

    @Test fun networkFailureStopsAndKeepsDirty() = runTest {
        val a = FakeAdapter("expenses").apply { local("e1", dirty = true); local("e2", dirty = true) }
        a.pushResults += Outcome.Failure(AppError.Offline)
        val report = ResourceSyncer(a).push()
        assertEquals(SyncProblem.Retry, report.problem)
        assertEquals(1, a.log.size)
        assertTrue(a.rows.getValue("e1").dirty && a.rows.getValue("e2").dirty)
    }

    @Test fun rejectedRecordDoesNotBlockTheQueue() = runTest {
        val a = FakeAdapter("expenses").apply { local("bad", dirty = true); local("good", dirty = true) }
        a.pushResults += Outcome.Failure(AppError.Server(400, "validation_failed", "x"))
        val report = ResourceSyncer(a).push()
        assertEquals(1, report.rejected)
        assertEquals(1, report.pushed)
        assertEquals("validation_failed", a.rows.getValue("bad").failure)
        assertEquals(false, a.rows.getValue("good").dirty)
        // a rejected record is skipped by later rounds until the user edits it
        a.log.clear()
        ResourceSyncer(a).push()
        assertTrue(a.log.isEmpty())
    }

    @Test fun unknownCategoryIsDeferredNotFailed() = runTest {
        val a = FakeAdapter("expenses").apply { local("e1", dirty = true) }
        a.pushResults += Outcome.Failure(AppError.Server(400, "unknown_category", "x"))
        val report = ResourceSyncer(a).push()
        assertEquals(1, report.deferred)
        assertNull(a.rows.getValue("e1").failure)
        assertTrue(a.rows.getValue("e1").dirty)
    }

    @Test fun serverErrorAndRateLimitRetryLater() = runTest {
        for (status in listOf(500, 503, 429)) {
            val a = FakeAdapter("e").apply { local("x", dirty = true) }
            a.pushResults += Outcome.Failure(AppError.Server(status, null, null))
            assertEquals(SyncProblem.Retry, ResourceSyncer(a).push().problem)
        }
    }

    @Test fun unauthorizedNeedsSignIn() = runTest {
        val a = FakeAdapter("e").apply { local("x", dirty = true) }
        a.pushResults += Outcome.Failure(AppError.Unauthorized)
        assertEquals(SyncProblem.AuthRequired, ResourceSyncer(a).push().problem)
        assertTrue(a.rows.getValue("x").dirty)
    }

    @Test fun firstPullLoadsEverythingAndStoresCursor() = runTest {
        val a = FakeAdapter("e")
        a.initial = Outcome.Success(InitialLoad(listOf(item("a", 1), item("b", 2)), "cursor-1"))
        val report = ResourceSyncer(a).pull()
        assertEquals(2, report.applied)
        assertEquals("cursor-1", a.cursor)
        assertEquals(2, a.rows.size)
    }

    @Test fun applyRules() = runTest {
        val a = FakeAdapter("e").apply {
            cursor = "c"
            local("dirty", version = 1, dirty = true, value = "mine")
            local("old", version = 5)
            local("gone", version = 1)
            local("newer", version = 1)
        }
        a.changes += page(
            item("dirty", 9), // ignored: the local edit wins
            item("old", 4), // older or equal: ignored
            item("gone", 2, deleted = true), // removed
            item("newer", 2), // overwritten
            item("fresh", 1), // inserted
            item("never-seen", 3, deleted = true), // nothing
            next = "c2",
        )
        val report = ResourceSyncer(a).pull()
        assertEquals(3, report.applied)
        assertEquals("mine", a.values["dirty"])
        assertEquals(5L, a.rows.getValue("old").version)
        assertNull(a.rows["gone"])
        assertEquals(2L, a.rows.getValue("newer").version)
        assertEquals(1L, a.rows.getValue("fresh").version)
        assertNull(a.rows["never-seen"])
        assertEquals("c2", a.cursor)
    }

    @Test fun followsPagesUntilNoMore() = runTest {
        val a = FakeAdapter("e").apply { cursor = "c" }
        a.changes += page(item("a", 1), next = "c1", more = true)
        a.changes += page(item("b", 1), next = "c2", more = false)
        val report = ResourceSyncer(a).pull()
        assertEquals(2, report.applied)
        assertEquals("c2", a.cursor)
    }

    @Test fun failedPullKeepsTheOldCursor() = runTest {
        val a = FakeAdapter("e").apply { cursor = "c" }
        a.changes += Outcome.Failure(AppError.Offline)
        val report = ResourceSyncer(a).pull()
        assertEquals(SyncProblem.Retry, report.problem)
        assertEquals("c", a.cursor)
    }

    @Test fun fullResyncKeepsDirtyRowsAndReloads() = runTest {
        val a = FakeAdapter("e").apply {
            cursor = "stale"
            local("clean", version = 1)
            local("dirty", version = 1, dirty = true)
        }
        a.changes += Outcome.Failure(AppError.Server(410, "full_resync_required", "x"))
        a.initial = Outcome.Success(InitialLoad(listOf(item("fresh", 7)), "c-new"))
        val report = ResourceSyncer(a).pull()
        assertTrue(report.resynced)
        assertNull(a.rows["clean"])
        assertTrue(a.rows.containsKey("dirty"))
        assertTrue(a.rows.containsKey("fresh"))
        assertEquals("c-new", a.cursor)
    }

    @Test fun engineRunsCategoriesFirstAndPushBeforePull() = runTest {
        val log = mutableListOf<String>()
        val categories = FakeAdapter("categories", log).apply { local("c1", dirty = true); cursor = "x"; changes += page() }
        val expenses = FakeAdapter("expenses", log).apply { local("e1", dirty = true); cursor = "x"; changes += page() }
        val result = SyncEngine(listOf(ResourceSyncer(categories), ResourceSyncer(expenses))).run()
        assertTrue(result.ok)
        assertEquals(listOf("categories:push:c1", "expenses:push:e1"), log)
        assertEquals(2, result.pushed)
    }

    @Test fun engineSkipsThePullWhenThePushCouldNotReachTheServer() = runTest {
        val a = FakeAdapter("categories").apply { local("c1", dirty = true); cursor = "x" }
        a.pushResults += Outcome.Failure(AppError.Offline)
        val result = SyncEngine(listOf(ResourceSyncer(a))).run()
        assertEquals(SyncProblem.Retry, result.problem)
        assertTrue(a.changes.isEmpty())
    }
}
