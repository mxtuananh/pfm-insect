@file:OptIn(ExperimentalMaterial3Api::class)

package hopper.pfm.insect.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutSmall
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DonutSmall
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import hopper.pfm.insect.R
import hopper.pfm.insect.core.ui.DefaultDropShadowConfig
import hopper.pfm.insect.core.ui.FloatingNavBar
import hopper.pfm.insect.core.ui.NavBarItem
import hopper.pfm.insect.core.ui.NoOp
import hopper.pfm.insect.core.ui.PlainTooltipWrapper
import hopper.pfm.insect.feature.auth.ProfileLinks
import hopper.pfm.insect.feature.auth.ProfileScreen
import hopper.pfm.insect.feature.budget.BudgetListScreen
import hopper.pfm.insect.feature.budget.BudgetScreenState
import hopper.pfm.insect.feature.expense.ExpenseListScreen
import hopper.pfm.insect.feature.report.ReportScreen

val tabs = listOf(
    NavBarItem(R.string.tab_expenses, Icons.Outlined.Payments, Icons.Filled.Payments),
    NavBarItem(R.string.tab_budgets, Icons.Outlined.DonutSmall, Icons.Filled.DonutSmall),
    NavBarItem(R.string.tab_reports, Icons.Outlined.BarChart, Icons.Filled.BarChart),
    NavBarItem(R.string.tab_profile, Icons.Outlined.Person, Icons.Filled.Person),
)

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
    onOpenNotifications: () -> Unit,
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val hazeState = rememberHazeState()
    val budgetScreenState = remember { BudgetScreenState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            MainShellTopAppBar(
                title = stringResource(tabs[selected].labelId),
                onOpenNotifications = onOpenNotifications,
                hazeState = hazeState,
            )
        },
        bottomBar = {
            FloatingBottomBar(
                selectedTabIndex = selected,
                onTabIndexSelected = { selected = it },
                hazeState = hazeState,
                onAddExpense = onAddExpense,
                onAddBudget = onAddBudget,
                canAddNewBudgetItem = budgetScreenState.canAddItem
            )
        },
        modifier = Modifier
            .fillMaxSize()
    ) { innerPadding ->

        AnimatedContent(
            targetState = selected,
            label = "MainShellContent",
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
        ) { selectedValue ->
            when (selectedValue) {
                0 -> ExpenseListScreen(
                    onAdd = onAddExpense,
                    onEdit = onEditExpense,
                    contentPadding = innerPadding,
                )

                1 -> BudgetListScreen(
                    onAdd = onAddBudget,
                    onEdit = onEditBudget,
                    contentPadding = innerPadding,
                    screenState = budgetScreenState
                )

                2 -> ReportScreen(
                    onCategory = onReportCategory,
                    contentPadding = innerPadding,
                )

                3 -> ProfileScreen(
                    onEdit = onEditProfile,
                    links = ProfileLinks(onCategories = onCategories, onHabits = onHabits),
                    contentPadding = innerPadding,
                )
            }
        }
    }
}

@Composable
private fun MainShellTopAppBar(
    title: String,
    onOpenNotifications: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val topbarBg = MaterialTheme.colorScheme.surface
    val topbarStyle = HazeStyle(
        backgroundColor = topbarBg,
        tint = HazeTint(topbarBg.copy(alpha = 0.5f)),
        blurRadius = 10.dp,
        fallbackTint = HazeTint(topbarBg.copy(alpha = 0.75f)),
    )

    Row(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .clip(CircleShape)
                .height(48.dp)
                .hazeEffect(style = topbarStyle, state = hazeState)
                .padding(horizontal = 20.dp)
                .wrapContentHeight(align = Alignment.CenterVertically),
        )

        Spacer(modifier = Modifier.weight(1f))

        SyncIndicator(
            modifier = Modifier
                .clip(CircleShape)
                .hazeEffect(style = topbarStyle, state = hazeState)
        )

        NotificationBell(
            modifier = Modifier
                .clip(CircleShape)
                .hazeEffect(style = topbarStyle, state = hazeState),
            onClick = onOpenNotifications
        )
    }
}

@Composable
fun FloatingBottomBar(
    selectedTabIndex: Int,
    onTabIndexSelected: (Int) -> Unit,
    hazeState: HazeState,
    onAddExpense: () -> Unit,
    onAddBudget: () -> Unit,
    canAddNewBudgetItem: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        FloatingNavBar(
            items = tabs,
            selectedIndex = selectedTabIndex,
            onSelect = onTabIndexSelected,
            hazeState = hazeState,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(vertical = 16.dp)
        )

        AnimatedVisibility(
            visible = when (selectedTabIndex) {
                0, 1 -> true
                else -> false
            },
            enter = fadeIn() + expandHorizontally(clip = false),
            exit = fadeOut() + shrinkHorizontally(clip = false)
        ) {
            PlainTooltipWrapper(text = "Add new item") {
                FloatingActionButton(
                    onClick = when (selectedTabIndex) {
                        0 -> onAddExpense
                        1 if (canAddNewBudgetItem) -> onAddBudget
                        else -> NoOp
                    },
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .padding(start = 12.dp, end = 4.dp)
                        .dropShadow(CircleShape, DefaultDropShadowConfig)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add new item")
                }
            }
        }
    }
}
