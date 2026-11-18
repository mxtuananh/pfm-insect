package hopper.pfm.insect

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import hopper.pfm.insect.core.model.SessionState
import hopper.pfm.insect.feature.auth.EditProfileScreen
import hopper.pfm.insect.feature.auth.LoginScreen
import hopper.pfm.insect.feature.auth.RegisterScreen
import hopper.pfm.insect.feature.auth.SessionExpiredScreen
import hopper.pfm.insect.feature.budget.BudgetEditScreen
import hopper.pfm.insect.feature.category.CategoryEditScreen
import hopper.pfm.insect.feature.category.CategoryListScreen
import hopper.pfm.insect.feature.expense.ExpenseFormScreen
import hopper.pfm.insect.feature.habit.HabitEditScreen
import hopper.pfm.insect.feature.report.ReportCategoryScreen
import hopper.pfm.insect.feature.habit.HabitListScreen
import hopper.pfm.insect.shell.MainShell
import hopper.pfm.insect.shell.SplashScreen
import hopper.pfm.insect.shell.StartDestination
import hopper.pfm.insect.shell.UpdateRequiredScreen
import kotlinx.serialization.Serializable

@Serializable
data object SplashKey : NavKey

@Serializable
data object LoginKey : NavKey

@Serializable
data object RegisterKey : NavKey

@Serializable
data object SessionExpiredKey : NavKey

@Serializable
data object MainKey : NavKey

@Serializable
data object EditProfileKey : NavKey

@Serializable
data object UpdateRequiredKey : NavKey

@Serializable
data class ExpenseFormKey(val expenseId: String? = null) : NavKey

@Serializable
data class BudgetEditKey(val budgetId: String? = null) : NavKey

@Serializable
data class ReportCategoryKey(val categoryId: String? = null, val from: String, val to: String) : NavKey

@Serializable
data object HabitsKey : NavKey

@Serializable
data class HabitEditKey(val habitId: String? = null) : NavKey

@Serializable
data object CategoriesKey : NavKey

@Serializable
data class CategoryEditKey(val categoryId: String? = null) : NavKey

/**
 * Root navigation. Splash checks GET /health (minAppVersion) and the stored session. Afterwards the session state
 * decides the root: signed out goes to Login, an expired session to the re-login screen, signed in to Main.
 */
@Composable
fun PfmApp(appViewModel: AppViewModel = hiltViewModel()) {
    val backStack = rememberNavBackStack(SplashKey)
    val context = LocalContext.current
    val session by appViewModel.session.collectAsStateWithLifecycle()

    // Follow session changes once the splash is over (and the app is not blocked by an update).
    LaunchedEffect(session) {
        val top = backStack.lastOrNull()
        if (top == SplashKey || top == UpdateRequiredKey) return@LaunchedEffect
        val root: NavKey = when (session) {
            SessionState.SignedIn -> MainKey
            SessionState.Expired -> SessionExpiredKey
            SessionState.SignedOut -> LoginKey
        }
        val inAuthArea = top == LoginKey || top == RegisterKey
        val rootChanged = when (root) {
            MainKey -> top != MainKey && top != EditProfileKey
            SessionExpiredKey -> top != SessionExpiredKey
            else -> !inAuthArea
        }
        if (rootChanged) {
            backStack.clear()
            backStack.add(root)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
        entryProvider = entryProvider {
            entry<SplashKey> {
                SplashScreen(
                    onDestination = { destination ->
                        backStack.clear()
                        backStack.add(
                            when (destination) {
                                StartDestination.Main -> MainKey
                                StartDestination.Login -> LoginKey
                                StartDestination.UpdateRequired -> UpdateRequiredKey
                            },
                        )
                    },
                )
            }
            entry<LoginKey> {
                LoginScreen(onRegister = { backStack.add(RegisterKey) })
            }
            entry<RegisterKey> {
                RegisterScreen(onLogin = { backStack.removeAt(backStack.lastIndex) })
            }
            entry<SessionExpiredKey> { SessionExpiredScreen() }
            entry<MainKey> {
                MainShell(
                    onEditProfile = { backStack.add(EditProfileKey) },
                    onAddExpense = { backStack.add(ExpenseFormKey()) },
                    onEditExpense = { backStack.add(ExpenseFormKey(it)) },
                    onCategories = { backStack.add(CategoriesKey) },
                    onHabits = { backStack.add(HabitsKey) },
                    onAddBudget = { backStack.add(BudgetEditKey()) },
                    onEditBudget = { backStack.add(BudgetEditKey(it)) },
                    onReportCategory = { id, from, to -> backStack.add(ReportCategoryKey(id, from, to)) },
                )
            }
            entry<ExpenseFormKey> { key ->
                ExpenseFormScreen(
                    expenseId = key.expenseId,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onNewCategory = { backStack.add(CategoryEditKey()) },
                    onDeleted = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<BudgetEditKey> { key ->
                BudgetEditScreen(budgetId = key.budgetId, onBack = { backStack.removeAt(backStack.lastIndex) })
            }
            entry<ReportCategoryKey> { key ->
                ReportCategoryScreen(
                    categoryId = key.categoryId,
                    from = key.from,
                    to = key.to,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onEdit = { backStack.add(ExpenseFormKey(it)) },
                )
            }
            entry<HabitsKey> {
                HabitListScreen(
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onEdit = { backStack.add(HabitEditKey(it)) },
                )
            }
            entry<HabitEditKey> { key ->
                HabitEditScreen(
                    habitId = key.habitId,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onNewCategory = { backStack.add(CategoryEditKey()) },
                )
            }
            entry<CategoriesKey> {
                CategoryListScreen(
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onEdit = { backStack.add(CategoryEditKey(it)) },
                )
            }
            entry<CategoryEditKey> { key ->
                CategoryEditScreen(categoryId = key.categoryId, onBack = { backStack.removeAt(backStack.lastIndex) })
            }
            entry<EditProfileKey> { EditProfileScreen(onBack = { backStack.removeAt(backStack.lastIndex) }) }
            entry<UpdateRequiredKey> { UpdateRequiredScreen(onUpdate = { openStore(context) }) }
        },
    )
}

private fun openStore(context: Context) {
    val id = context.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id"))
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id"))
    try {
        context.startActivity(market)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(web)
    }
}
