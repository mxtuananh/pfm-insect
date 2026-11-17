package hopper.pfm.insect.feature.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hopper.pfm.insect.core.model.Category
import hopper.pfm.insect.core.model.SyncStatus
import hopper.pfm.insect.core.ui.CategoryAvatar
import hopper.pfm.insect.core.ui.CategoryColors
import hopper.pfm.insect.core.ui.CategoryIcons
import hopper.pfm.insect.core.ui.PfmPrimaryButton
import hopper.pfm.insect.core.ui.PfmTextField

@Composable
fun CategoryListScreen(
    onBack: () -> Unit,
    onEdit: (categoryId: String?) -> Unit,
    viewModel: CategoryListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showArchived by rememberSaveable { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.category_back))
                    }
                    Text(stringResource(R.string.category_title), style = MaterialTheme.typography.titleLarge)
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.active, key = { it.id }) { category ->
                        CategoryRow(category, subtitle = null, onClick = { onEdit(category.id) })
                    }
                    if (state.archived.isNotEmpty()) {
                        item(key = "archived-header") {
                            Row(
                                Modifier.fillMaxWidth().clickable { showArchived = !showArchived }.padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    stringResource(R.string.category_archived_section, state.archived.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    if (showArchived) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (showArchived) {
                            items(state.archived, key = { it.id }) { category ->
                                CategoryRow(
                                    category,
                                    subtitle = stringResource(R.string.category_archived_hint),
                                    onClick = { onEdit(category.id) },
                                    dim = true,
                                )
                            }
                        }
                    }
                }
            }
            ExtendedFloatingActionButton(
                onClick = { onEdit(null) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.category_add)) },
                shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp),
            )
        }
    }
}

@Composable
private fun CategoryRow(category: Category, subtitle: String?, onClick: () -> Unit, dim: Boolean = false) {
    val syncText = when (category.syncStatus) {
        SyncStatus.PENDING -> stringResource(R.string.category_pending)
        SyncStatus.FAILED -> stringResource(R.string.category_rejected)
        SyncStatus.SYNCED -> null
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = if (dim) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryAvatar(category.icon, category.color)
            Column(Modifier.weight(1f)) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (dim) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                val second = listOfNotNull(subtitle, syncText).joinToString(" · ")
                if (second.isNotEmpty()) {
                    Text(
                        second,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (category.syncStatus == SyncStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryEditScreen(
    categoryId: String?,
    onBack: () -> Unit,
    viewModel: CategoryEditViewModel = hiltViewModel<CategoryEditViewModel, CategoryEditViewModel.Factory>(
        key = "category-$categoryId",
        creationCallback = { factory -> factory.create(categoryId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.category_back))
                }
                Text(
                    stringResource(if (state.isNew) R.string.category_new_title else R.string.category_edit_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CategoryAvatar(state.icon, state.color, size = 64.dp)
                Spacer(Modifier.height(24.dp))
                PfmTextField(
                    value = state.name,
                    onValueChange = viewModel::onName,
                    label = stringResource(R.string.category_name),
                    errorText = if (state.nameError) stringResource(R.string.category_name_required) else null,
                    supportingText = stringResource(R.string.category_name_hint),
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::save,
                )
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.category_icon))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CategoryIcons.ids.forEach { id ->
                        val selected = id == state.icon
                        Box(
                            Modifier.size(44.dp)
                                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
                                .then(if (selected) Modifier.androidBorder() else Modifier)
                                .clickable { viewModel.onIcon(id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(CategoryIcons.vector(id), contentDescription = id, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.category_color))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    CategoryColors.palette.forEach { hex ->
                        val selected = hex.equals(state.color, ignoreCase = true)
                        Box(
                            Modifier.size(36.dp)
                                .background(CategoryColors.parse(hex), CircleShape)
                                .then(if (selected) Modifier.androidBorder(3) else Modifier)
                                .clickable { viewModel.onColor(hex) },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.category_archive), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.category_archive_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.archived, onCheckedChange = viewModel::onArchived)
                    }
                }
                Text(
                    stringResource(R.string.category_no_delete),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp),
                )
            }
            PfmPrimaryButton(
                text = stringResource(R.string.category_save),
                onClick = viewModel::save,
                enabled = state.loaded,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp),
    )
}

/** Selection ring in the primary colour. */
@Composable
private fun Modifier.androidBorder(widthDp: Int = 2): Modifier =
    this.then(Modifier.border(BorderStroke(widthDp.dp, MaterialTheme.colorScheme.primary), CircleShape))
