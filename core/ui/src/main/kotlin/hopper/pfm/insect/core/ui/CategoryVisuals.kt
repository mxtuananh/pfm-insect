package hopper.pfm.insect.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The server stores an icon id and a "#RRGGBB" colour for each category and does not interpret them, so the ids below
 * are the app's own vocabulary. Unknown ids (from another client) fall back to a generic icon.
 */
object CategoryIcons {
    private val icons: LinkedHashMap<String, ImageVector> = linkedMapOf(
        "food" to Icons.Filled.Restaurant,
        "transport" to Icons.Filled.DirectionsBus,
        "shopping" to Icons.Filled.ShoppingBag,
        "health" to Icons.Filled.MedicalServices,
        "entertainment" to Icons.Filled.Movie,
        "bills" to Icons.Filled.Receipt,
        "gift" to Icons.Filled.CardGiftcard,
        "education" to Icons.Filled.School,
        "home" to Icons.Filled.Home,
        "coffee" to Icons.Filled.LocalCafe,
        "travel" to Icons.Filled.Flight,
        "work" to Icons.Filled.Work,
        "pets" to Icons.Filled.Pets,
        "fitness" to Icons.Filled.FitnessCenter,
        "other" to Icons.Filled.MoreHoriz,
    )

    /** Ids offered in the icon picker, in display order. */
    val ids: List<String> get() = icons.keys.toList()

    fun vector(id: String?): ImageVector = icons[id] ?: Icons.Filled.Category
}

object CategoryColors {
    /** Swatches offered in the colour picker (the server defaults come first). */
    val palette: List<String> = listOf(
        "#FF8A65", "#4FC3F7", "#BA68C8", "#81C784", "#F06292", "#FFD54F", "#E57373", "#7986CB",
        "#A1887F", "#90A4AE", "#4DB6AC", "#AED581",
    )

    const val DEFAULT = "#90A4AE"

    fun parse(hex: String?): Color {
        val text = hex?.removePrefix("#") ?: return Color(0xFF90A4AE)
        return text.toLongOrNull(16)?.takeIf { text.length == 6 }?.let { Color(0xFF000000 or it) } ?: Color(0xFF90A4AE)
    }

    /** Dark or light content, whichever reads better on the swatch. */
    fun onColor(background: Color): Color = if (background.luminance() > 0.5f) Color(0xFF1B1B1B) else Color.White

    private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

/** Round avatar of a category: its icon on its colour. */
@Composable
fun CategoryAvatar(icon: String?, color: String?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val background = CategoryColors.parse(color)
    Box(modifier = modifier.size(size).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = CategoryIcons.vector(icon),
            contentDescription = null,
            tint = CategoryColors.onColor(background),
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
