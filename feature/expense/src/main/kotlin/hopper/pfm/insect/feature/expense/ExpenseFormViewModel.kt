package hopper.pfm.insect.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.common.Money
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.domain.expense.CategoryRepository
import hopper.pfm.insect.core.domain.expense.ExpenseRepository
import hopper.pfm.insect.core.model.Category
import hopper.pfm.insect.core.model.Expense
import hopper.pfm.insect.core.model.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

const val MAX_TITLE = 100
const val MAX_NOTE = 500
private val YearRange = 2000..2100

enum class FormError { AmountRequired, TitleRequired, DateOutOfRange }

data class ExpenseFormUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val currency: String = "VND",
    /** What the user typed for the amount (digits, grouped on display). */
    val amountText: String = "",
    val title: String = "",
    val categoryId: String? = null,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    /** Errors are only shown after the first attempt to save, so an empty new form is not red. */
    val showErrors: Boolean = false,
    val saved: Boolean = false,
    val deleted: Expense? = null,
) {
    val amount: Long? get() = Money.parse(amountText, currency)?.takeIf { it > 0 }

    val errors: Set<FormError>
        get() = buildSet {
            if (amount == null) add(FormError.AmountRequired)
            if (title.trim().isEmpty()) add(FormError.TitleRequired)
            if (date.year !in YearRange) add(FormError.DateOutOfRange)
        }

    val canSave: Boolean get() = loaded && errors.isEmpty()
}

/** expenseId null = a new expense; a new expense gets its UUID here, on the device (that is what allows offline creation). */
@HiltViewModel(assistedFactory = ExpenseFormViewModel.Factory::class)
class ExpenseFormViewModel @AssistedInject constructor(
    @Assisted private val expenseId: String?,
    private val expenses: ExpenseRepository,
    categories: CategoryRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(expenseId: String?): ExpenseFormViewModel
    }

    private val _state = MutableStateFlow(ExpenseFormUiState())
    val state: StateFlow<ExpenseFormUiState> = _state.asStateFlow()

    private var original: Expense? = null

    init {
        viewModelScope.launch {
            val currency = auth.profile.first()?.currency ?: "VND"
            val existing = expenseId?.let { expenses.get(it) }
            original = existing
            _state.update {
                if (existing == null) {
                    it.copy(loaded = true, currency = currency)
                } else {
                    it.copy(
                        loaded = true,
                        isNew = false,
                        currency = currency,
                        amountText = Money.format(existing.amount, currency),
                        title = existing.title,
                        categoryId = existing.categoryId,
                        date = existing.spentOn,
                        note = existing.note.orEmpty(),
                        syncStatus = existing.syncStatus,
                    )
                }
            }
        }
        viewModelScope.launch {
            categories.observeAll().collect { all ->
                // Archived categories are not offered for new choices, but the one already used stays selectable.
                _state.update { s ->
                    s.copy(categories = all.filter { !it.archived || it.id == s.categoryId }.sortedBy { it.name.lowercase() })
                }
            }
        }
    }

    fun onAmount(value: String) = _state.update { s ->
        val text = if (Money.decimals(s.currency) == 0) {
            // Dong has no decimals: digits only, shown with grouping dots as the user types.
            val digits = value.filter { it.isDigit() }
            if (digits.isEmpty()) "" else Money.parse(digits, s.currency)?.let { Money.format(it, s.currency) } ?: s.amountText
        } else {
            // Cents: digits and one dot, at most two decimals; a trailing dot is allowed while typing.
            val cleaned = value.filter { it.isDigit() || it == '.' }
            if (cleaned.isEmpty() || Money.parse(cleaned.trimEnd('.').ifEmpty { "0" }, s.currency) != null && cleaned.count { it == '.' } <= 1) cleaned else s.amountText
        }
        s.copy(amountText = text)
    }

    fun onTitle(value: String) = _state.update { it.copy(title = value.take(MAX_TITLE)) }

    fun onCategory(id: String?) = _state.update { it.copy(categoryId = if (it.categoryId == id) null else id) }

    fun onDate(date: LocalDate) = _state.update { it.copy(date = date) }

    fun onNote(value: String) = _state.update { it.copy(note = value.take(MAX_NOTE)) }

    fun save() {
        val s = _state.value
        val amount = s.amount
        if (!s.canSave || amount == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            expenses.save(
                Expense(
                    id = expenseId ?: UUID.randomUUID().toString(),
                    title = s.title,
                    note = s.note,
                    amount = amount,
                    spentOn = s.date,
                    categoryId = s.categoryId,
                ),
            )
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val existing = original ?: return
        viewModelScope.launch {
            expenses.delete(existing.id)
            _state.update { it.copy(deleted = existing) }
        }
    }
}
