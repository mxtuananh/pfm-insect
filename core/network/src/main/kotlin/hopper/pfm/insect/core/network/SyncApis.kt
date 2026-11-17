package hopper.pfm.insect.core.network

import hopper.pfm.insect.core.common.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

const val CHANGES_PAGE_SIZE = 200

class CategoryApi @Inject constructor(private val client: HttpClient) {
    suspend fun list(): Outcome<ListResponse<CategoryDto>> = client.callApi { get("/v1/categories") }

    suspend fun changes(cursor: String): Outcome<ChangesResponse<CategoryDto>> = client.callApi {
        get("/v1/categories/changes") {
            parameter("cursor", cursor)
            parameter("limit", CHANGES_PAGE_SIZE)
        }
    }

    suspend fun put(id: String, body: UpsertCategoryRequest): Outcome<CategoryDto> = client.callApi {
        put("/v1/categories/$id") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }
}

class ExpenseApi @Inject constructor(private val client: HttpClient) {
    /** Newest first; pass the nextCursor of the previous page as before to get older ones. */
    suspend fun page(before: String?): Outcome<ExpensePage> = client.callApi {
        get("/v1/expenses") {
            parameter("limit", CHANGES_PAGE_SIZE)
            before?.let { parameter("before", it) }
        }
    }

    suspend fun changes(cursor: String): Outcome<ChangesResponse<ExpenseDto>> = client.callApi {
        get("/v1/expenses/changes") {
            parameter("cursor", cursor)
            parameter("limit", CHANGES_PAGE_SIZE)
        }
    }

    suspend fun put(id: String, body: UpsertExpenseRequest): Outcome<ExpenseDto> = client.callApi {
        put("/v1/expenses/$id") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun delete(id: String): Outcome<Unit> = client.callApiUnit { delete("/v1/expenses/$id") }
}

class HabitApi @Inject constructor(private val client: HttpClient) {
    suspend fun list(): Outcome<ListResponse<HabitDto>> = client.callApi { get("/v1/habits") }

    suspend fun changes(cursor: String): Outcome<ChangesResponse<HabitDto>> = client.callApi {
        get("/v1/habits/changes") {
            parameter("cursor", cursor)
            parameter("limit", CHANGES_PAGE_SIZE)
        }
    }

    suspend fun put(id: String, body: UpsertHabitRequest): Outcome<HabitDto> = client.callApi {
        put("/v1/habits/$id") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun delete(id: String): Outcome<Unit> = client.callApiUnit { delete("/v1/habits/$id") }
}

class BudgetApi @Inject constructor(private val client: HttpClient) {
    suspend fun list(): Outcome<ListResponse<BudgetDto>> = client.callApi { get("/v1/budgets") }

    suspend fun changes(cursor: String): Outcome<ChangesResponse<BudgetDto>> = client.callApi {
        get("/v1/budgets/changes") {
            parameter("cursor", cursor)
            parameter("limit", CHANGES_PAGE_SIZE)
        }
    }

    suspend fun put(id: String, body: UpsertBudgetRequest): Outcome<BudgetDto> = client.callApi {
        put("/v1/budgets/$id") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun delete(id: String): Outcome<Unit> = client.callApiUnit { delete("/v1/budgets/$id") }
}
