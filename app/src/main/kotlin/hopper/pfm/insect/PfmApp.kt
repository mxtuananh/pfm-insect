package hopper.pfm.insect

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import hopper.pfm.insect.shell.MainShell
import hopper.pfm.insect.shell.SplashScreen
import hopper.pfm.insect.shell.UpdateRequiredScreen
import kotlinx.serialization.Serializable

@Serializable
data object SplashKey : NavKey

@Serializable
data object MainKey : NavKey

@Serializable
data object UpdateRequiredKey : NavKey

/**
 * Root navigation. Splash will later check the session and GET /health (minAppVersion): signed-out users go to
 * Auth (Phase 3) and unsupported versions to UpdateRequiredKey (Phase 2).
 */
@Composable
fun PfmApp() {
    val backStack = rememberNavBackStack(SplashKey)
    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
        entryProvider = entryProvider {
            entry<SplashKey> {
                SplashScreen(
                    onFinished = {
                        backStack.clear()
                        backStack.add(MainKey)
                    },
                )
            }
            entry<MainKey> { MainShell() }
            entry<UpdateRequiredKey> { UpdateRequiredScreen(onUpdate = {}) }
        },
    )
}
