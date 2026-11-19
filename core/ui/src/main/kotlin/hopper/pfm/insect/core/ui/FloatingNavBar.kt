package hopper.pfm.insect.core.ui

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** Bottom padding that scrolling content must reserve so nothing is hidden behind the floating bar. */
val FloatingBarContentPadding = 120.dp

data class NavBarItem(
    @StringRes val labelId: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

private val BarHeight = 72.dp
private val SlotWidth = 80.dp
private val SlotHeight = 64.dp

/**
 * Floating pill navigation bar (approved design): 72dp high, 16dp margin, surfaceContainer ~72% over a 24dp
 * background blur (API 31+); on older devices the tint is ~92% opaque. The selected indicator fills the whole slot.
 *
 * [hazeState] must be shared with the scrolling content, which marks itself with `Modifier.hazeSource(hazeState)`.
 */
@Composable
fun FloatingNavBar(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val container = MaterialTheme.colorScheme.surfaceContainer
    val style = HazeStyle(
        backgroundColor = container,
        tint = HazeTint(container.copy(alpha = 0.5f)),
        blurRadius = 10.dp,
        fallbackTint = HazeTint(container.copy(alpha = 0.75f)),
    )
    val selectedBackground = MaterialTheme.colorScheme.secondaryContainer
    val dpOffsetX by animateDpAsState(
        targetValue = SlotWidth * selectedIndex,
        label = "dpOffsetX",
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(16.dp)
            .dropShadow(CircleShape) {
                this.color = DefaultShadowColor
                this.offset = Offset(x = 0f, y = 4f)
                this.radius = 14f
                this.alpha = 0.45f
            }
            .clip(CircleShape)
            .height(BarHeight)
            .hazeEffect(state = hazeState, style = style),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .drawBehind {
                    val left = dpOffsetX.toPx()
                    drawRoundRect(
                        color = selectedBackground,
                        topLeft = Offset(x = left, y = 0f),
                        size = size.copy(width = SlotWidth.toPx()),
                        cornerRadius = CornerRadius(size.height / 2f)
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                NavSlot(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}

@Composable
private fun NavSlot(item: NavBarItem, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val contentColor by animateColorAsState (
        targetValue = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        label = "contentColor"
    )
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .width(SlotWidth)
            .height(SlotHeight)
            .clickable(enabled = !selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(item.labelId),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Space a scrolling screen must keep free at its bottom: the floating bar plus the system navigation bar. */
@Composable
fun floatingBarSpace(): androidx.compose.ui.unit.Dp =
    FloatingBarContentPadding + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
