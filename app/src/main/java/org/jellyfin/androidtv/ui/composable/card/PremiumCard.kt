package org.jellyfin.androidtv.ui.composable.card

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme

/**
 * Base premium card with focus-aware animations.
 * Scales up smoothly on focus with a glowing border and elevated shadow.
 */
@Composable
fun PremiumCard(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	onLongFocus: (() -> Unit)? = null,
	onFocusChanged: ((Boolean) -> Unit)? = null,
	content: @Composable (focused: Boolean) -> Unit,
) {
	var focused by remember { mutableStateOf(false) }

	val scale by animateFloatAsState(
		targetValue = if (focused) 1.05f else 1f,
		animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
		label = "cardScale",
	)

	val elevation by animateDpAsState(
		targetValue = if (focused) 16.dp else 0.dp,
		animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
		label = "cardElevation",
	)

	val borderWidth by animateDpAsState(
		targetValue = if (focused) 2.dp else 0.dp,
		animationSpec = tween(150),
		label = "cardBorder",
	)

	val shape = if (focused) JellyfinTheme.shapes.cardFocused else JellyfinTheme.shapes.card
	val accentColor = JellyfinTheme.colorScheme.focusRing
	val borderColor by animateColorAsState(
		targetValue = if (focused) accentColor else Color.Transparent,
		animationSpec = tween(200),
		label = "borderColor",
	)

	// Outer box has fixed padding — prevents LazyRow layout shifts
	Box(
		modifier = modifier
			.padding(6.dp)
			.onFocusChanged { state ->
				focused = state.isFocused
				onFocusChanged?.invoke(state.isFocused)
			}
			.focusable()
			.onKeyEvent { event ->
				if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.DirectionCenter)) {
					onClick()
					true
				} else false
			}
	) {
		// Card content with visual-only transforms
		Box(
			modifier = Modifier
				.graphicsLayer {
					scaleX = scale
					scaleY = scale
					shadowElevation = elevation.toPx()
					this.shape = shape
					clip = true
				}
				.border(BorderStroke(borderWidth, borderColor), shape)
		) {
			content(focused)
		}
	}
}
