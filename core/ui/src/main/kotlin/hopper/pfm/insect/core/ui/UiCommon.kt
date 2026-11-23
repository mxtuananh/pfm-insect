package hopper.pfm.insect.core.ui

import androidx.compose.ui.draw.DropShadowScope
import androidx.compose.ui.geometry.Offset

val NoOp = {}
val DefaultDropShadowConfig: DropShadowScope.() -> Unit = {
    offset = Offset(0f, 6f)
    radius = 8f
    alpha = 0.5f
}