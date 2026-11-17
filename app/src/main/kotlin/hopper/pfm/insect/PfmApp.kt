package hopper.pfm.insect

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import hopper.pfm.insect.shell.MainShell
import hopper.pfm.insect.shell.SplashScreen
import hopper.pfm.insect.shell.StartDestination
import hopper.pfm.insect.shell.UpdateRequiredScreen
import kotlinx.serialization.Serializable

@Serializable
data object SplashKey : NavKey

@Serializable
data object MainKey : NavKey

@Serializable
data object UpdateRequiredKey : NavKey

/**
 * Root navigation. Splash checks GET /health (minAppVersion); signed-out users will go to Auth in phase 3.
 */
@Composable
fun PfmApp() {
    val backStack = rememberNavBackStack(SplashKey)
    val context = LocalContext.current
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
                                StartDestination.UpdateRequired -> UpdateRequiredKey
                            },
                        )
                    },
                )
            }
            entry<MainKey> { MainShell() }
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
