package hopper.pfm.insect.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hopper.pfm.insect.core.common.AuthTokens
import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.data.sync.BudgetSyncAdapter
import hopper.pfm.insect.core.data.sync.CategorySyncAdapter
import hopper.pfm.insect.core.data.sync.ExpenseSyncAdapter
import hopper.pfm.insect.core.data.sync.HabitSyncAdapter
import hopper.pfm.insect.core.database.ExpenseEntity
import hopper.pfm.insect.core.database.PfmDatabase
import hopper.pfm.insect.core.domain.sync.ResourceSyncer
import hopper.pfm.insect.core.domain.sync.SyncEngine
import hopper.pfm.insect.core.network.ApiConfig
import hopper.pfm.insect.core.network.AuthApi
import hopper.pfm.insect.core.network.BudgetApi
import hopper.pfm.insect.core.network.CategoryApi
import hopper.pfm.insect.core.network.ExpenseApi
import hopper.pfm.insect.core.network.HabitApi
import hopper.pfm.insect.core.network.TokenProvider
import hopper.pfm.insect.core.network.UpsertExpenseRequest
import hopper.pfm.insect.core.network.createApiClient
import hopper.pfm.insect.core.network.installBearerAuth
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Runs the real sync engine on a real Room database against the dev server (http://localhost:8080, reached through
 * "adb reverse tcp:8080 tcp:8080"; see docs/SERVER.md). Each test registers a fresh account.
 */
@RunWith(AndroidJUnit4::class)
class SyncAgainstServerTest {

    private class Tokens(var current: AuthTokens?) : TokenProvider {
        override suspend fun tokens() = current
        override suspend fun save(tokens: AuthTokens) { current = tokens }
        override suspend fun onRefreshRejected() { current = null }
    }

    private lateinit var db: PfmDatabase
    private lateinit var engine: SyncEngine
    private lateinit var expenseApi: ExpenseApi
    private lateinit var categoryApi: CategoryApi

    @Before fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PfmDatabase::class.java).build()
        val config = ApiConfig("http://localhost:8080")
        val bootstrap = AuthApi(createApiClient(OkHttp.create(), config))
        val email = "sync-${UUID.randomUUID().toString().take(8)}@example.com"
        assertTrue(bootstrap.register(email, "Passw0rdX") is Outcome.Success)
        val login = bootstrap.login(email, "Passw0rdX", "androidtest") as Outcome.Success
        val tokens = Tokens(AuthTokens(login.value.accessToken, login.value.refreshToken))
        val client = createApiClient(OkHttp.create(), config) { installBearerAuth(tokens) }
        expenseApi = ExpenseApi(client)
        categoryApi = CategoryApi(client)
        engine = SyncEngine(
            listOf(
                ResourceSyncer(CategorySyncAdapter(db.categoryDao(), categoryApi, db.syncCursorDao(), db)),
                ResourceSyncer(ExpenseSyncAdapter(db.expenseDao(), expenseApi, db.syncCursorDao(), db)),
                ResourceSyncer(HabitSyncAdapter(db.habitDao(), HabitApi(client), db.syncCursorDao(), db)),
                ResourceSyncer(BudgetSyncAdapter(db.budgetDao(), BudgetApi(client), db.syncCursorDao(), db)),
            ),
        )
    }

    @After fun tearDown() { db.close() }

    private fun expense(id: String, title: String, amount: Long = 45_000, category: String?, rev: Long = 1, deleted: Boolean = false, version: Long = 0, dirty: Boolean = true) =
        ExpenseEntity(id, title, null, amount, "2026-11-17", category, version = version, deleted = deleted, dirty = dirty, localRev = rev)

    @Test fun firstSyncLoadsTheDefaultCategories() = runBlocking {
        val result = engine.run()
        assertTrue(result.problem.toString(), result.ok)
        assertEquals(10, result.applied)
        assertNotNull(db.syncCursorDao().get("categories"))
        assertNotNull(db.syncCursorDao().get("expenses"))
    }

    @Test fun localExpenseIsPushedAndThenServerChangesAreAdopted() = runBlocking {
        engine.run()
        val category = (categoryApi.list() as Outcome.Success).value.items.first().id

        // 1. Create offline, push.
        val id = UUID.randomUUID().toString()
        db.expenseDao().upsert(expense(id, "Pho bo", category = category))
        val pushed = engine.run()
        assertTrue(pushed.ok)
        assertEquals(1, pushed.pushed)
        val stored = db.expenseDao().get(id)!!
        assertFalse(stored.dirty)
        assertTrue(stored.version > 0)
        val onServer = (expenseApi.page(null) as Outcome.Success).value.items
        assertEquals(listOf("Pho bo"), onServer.map { it.title })

        // 2. Another device changes it: the next round adopts the new copy.
        expenseApi.put(id, UpsertExpenseRequest("Pho bo dac biet", null, 60_000, "2026-11-17", category))
        engine.run()
        assertEquals("Pho bo dac biet", db.expenseDao().get(id)!!.title)

        // 3. A dirty local edit wins over the server copy.
        db.expenseDao().upsert(db.expenseDao().get(id)!!.copy(title = "Local edit", dirty = true, localRev = 2))
        engine.run()
        assertEquals("Local edit", (expenseApi.page(null) as Outcome.Success).value.items.single().title)

        // 4. Delete: sent as DELETE, then the row disappears locally and on the server.
        db.expenseDao().upsert(db.expenseDao().get(id)!!.copy(deleted = true, dirty = true, localRev = 3))
        val deleted = engine.run()
        assertTrue(deleted.ok)
        assertNull(db.expenseDao().get(id))
        assertTrue((expenseApi.page(null) as Outcome.Success).value.items.isEmpty())
    }

    @Test fun rejectedRecordIsMarkedAndDoesNotBlockOthers() = runBlocking {
        engine.run()
        val category = (categoryApi.list() as Outcome.Success).value.items.first().id
        val bad = UUID.randomUUID().toString()
        val good = UUID.randomUUID().toString()
        db.expenseDao().upsert(expense(bad, "Negative", amount = -5, category = category))
        db.expenseDao().upsert(expense(good, "Fine", category = category))
        val result = engine.run()
        assertEquals(1, result.pushed)
        assertEquals(1, result.rejected)
        assertEquals("validation_failed", db.expenseDao().get(bad)!!.failure)
        assertTrue(db.expenseDao().get(bad)!!.dirty)
        assertFalse(db.expenseDao().get(good)!!.dirty)
    }

    @Test fun expenseWithUnknownCategoryWaitsForTheCategory() = runBlocking {
        engine.run()
        val id = UUID.randomUUID().toString()
        db.expenseDao().upsert(expense(id, "Orphan", category = UUID.randomUUID().toString()))
        val result = engine.run()
        assertTrue(result.ok)
        assertEquals(0, result.rejected)
        assertNull(db.expenseDao().get(id)!!.failure)
        assertTrue(db.expenseDao().get(id)!!.dirty)
    }
}
