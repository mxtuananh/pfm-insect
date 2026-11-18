package hopper.pfm.insect.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutSmall
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DonutSmall
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import hopper.pfm.insect.R
import hopper.pfm.insect.core.ui.floatingBarSpace
import hopper.pfm.insect.core.ui.FloatingNavBar
import hopper.pfm.insect.core.ui.NavBarItem
import hopper.pfm.insect.feature.auth.ProfileLinks
import hopper.pfm.insect.feature.budget.BudgetListScreen
import hopper.pfm.insect.feature.auth.ProfileScreen
import hopper.pfm.insect.feature.expense.ExpenseListScreen
import hopper.pfm.insect.feature.report.ReportScreen

/**
 * Main shell: four tabs, a top bar with sync status and notifications, and the floating bottom bar over the content.
 * Tab content is a placeholder until the feature modules arrive (Expense in Phase 5, Budget in 7, Report in 8).
 */
@Composable
fun MainShell(
    onEditProfile: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (String) -> Unit,
    onCategories: () -> Unit,
    onHabits: () -> Unit,
    onAddBudget: () -> Unit,
    onEditBudget: (String) -> Unit,
    onReportCategory: (String?, String, String) -> Unit,
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val hazeState = rememberHazeState()
    val tabs = listOf(
        NavBarItem(stringResource(R.string.tab_expenses), Icons.Outlined.Payments, Icons.Filled.Payments),
        NavBarItem(stringResource(R.string.tab_budgets), Icons.Outlined.DonutSmall, Icons.Filled.DonutSmall),
        NavBarItem(stringResource(R.string.tab_reports), Icons.Outlined.BarChart, Icons.Filled.BarChart),
        NavBarItem(stringResource(R.string.tab_profile), Icons.Outlined.Person, Icons.Filled.Person),
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().hazeSource(hazeState)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = tabs[selected].label,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                SyncIndicator()
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.notifications))
                }
            }
            if (selected == 0) {
                ExpenseListScreen(
                    onAdd = onAddExpense,
                    onEdit = onEditExpense,
                    bottomBarSpace = floatingBarSpace(),
                    modifier = Modifier.weight(1f),
                )
            } else if (selected == 1) {
                BudgetListScreen(
                    onAdd = onAddBudget,
                    onEdit = onEditBudget,
                    bottomBarSpace = floatingBarSpace(),
                    modifier = Modifier.weight(1f),
                )
            } else if (selected == 2) {
                ReportScreen(
                    onCategory = onReportCategory,
                    bottomBarSpace = floatingBarSpace(),
                    modifier = Modifier.weight(1f),
                )
            } else if (selected == 3) {
                ProfileScreen(
                    onEdit = onEditProfile,
                    links = ProfileLinks(onCategories = onCategories, onHabits = onHabits),
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = floatingBarSpace()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.placeholder_coming_soon),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        FloatingNavBar(
            items = tabs,
            selectedIndex = selected,
            onSelect = { selected = it },
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
