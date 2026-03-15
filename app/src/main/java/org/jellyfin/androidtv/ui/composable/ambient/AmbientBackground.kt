package org.jellyfin.androidtv.ui.composable.ambient

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.StateFlow

/**
 * Renders a subtle radial gradient background tinted with a dominant color
 * extracted from the currently focused content's artwork.
 * The color transitions smoothly for a premium ambient effect.
 */
@Composable
fun AmbientBackground(
	dominantColor: StateFlow<Color>,
	modifier: Modifier = Modifier,
) {
	val rawColor by dominantColor.collectAsState()

	val animatedColor by animateColorAsState(
		targetValue = rawColor.copy(alpha = 0.15f),
		animationSpec = tween(durationMillis = 600),
		label = "ambientColor",
	)

	Box(
		modifier = modifier
			.fillMaxSize()
			.background(
				Brush.radialGradient(
					colors = listOf(
						animatedColor,
						Color.Transparent,
					),
					radius = 1200f,
				)
			),
	)
}
