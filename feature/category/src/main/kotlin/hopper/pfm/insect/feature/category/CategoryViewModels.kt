package hopper.pfm.insect.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.domain.expense.CategoryRepository
import hopper.pfm.insect.core.model.Category
import hopper.pfm.insect.core.ui.CategoryColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CategoryListUiState(
    val loaded: Boolean = false,
    val active: List<Category> = emptyList(),
    val archived: List<Category> = emptyList(),
)

@HiltViewModel
class CategoryListViewModel @Inject constructor(repository: CategoryRepository) : ViewModel() {
    val state: StateFlow<CategoryListUiState> = repository.observeAll()
        .map { all ->
            val sorted = all.sortedBy { it.name.lowercase() }
            CategoryListUiState(loaded = true, active = sorted.filter { !it.archived }, archived = sorted.filter { it.archived })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryListUiState())
}

data class CategoryEditUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val icon: String = "other",
    val color: String = CategoryColors.palette.first(),
    val archived: Boolean = false,
    val nameError: Boolean = false,
    val saved: Boolean = false,
)

/** The category id comes from navigation (null = a new category); a new category gets its UUID here, on the device. */
@HiltViewModel(assistedFactory = CategoryEditViewModel.Factory::class)
class CategoryEditViewModel @AssistedInject constructor(
    @Assisted private val editedId: String?,
    private val repository: CategoryRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(categoryId: String?): CategoryEditViewModel
    }

    private val _state = MutableStateFlow(CategoryEditUiState())
    val state: StateFlow<CategoryEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = editedId?.let { repository.get(it) }
            _state.value = if (existing == null) {
                CategoryEditUiState(loaded = true)
            } else {
                CategoryEditUiState(
                    loaded = true,
                    isNew = false,
                    name = existing.name,
                    icon = existing.icon ?: "other",
                    color = existing.color ?: CategoryColors.DEFAULT,
                    archived = existing.archived,
                )
            }
        }
    }

    fun onName(value: String) = _state.update { it.copy(name = value.take(MAX_NAME), nameError = false) }
    fun onIcon(value: String) = _state.update { it.copy(icon = value) }
    fun onColor(value: String) = _state.update { it.copy(color = value) }
    fun onArchived(value: Boolean) = _state.update { it.copy(archived = value) }

    fun save() {
        val s = _state.value
        if (s.name.trim().isEmpty()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            repository.save(
                Category(
                    id = editedId ?: UUID.randomUUID().toString(),
                    name = s.name,
                    icon = s.icon,
                    color = s.color,
                    archived = s.archived,
                ),
            )
            _state.update { it.copy(saved = true) }
        }
    }

    companion object {
        const val MAX_NAME = 50
    }
}
