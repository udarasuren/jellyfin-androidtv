package org.jellyfin.androidtv.ui.composable.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text

/**
 * 16:9 landscape card for continue watching, episodes, and recently added.
 * Shows a progress bar overlay and title/subtitle that reveal on focus.
 */
@Composable
fun LandscapeCard(
	imageUrl: String?,
	title: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	subtitle: String? = null,
	progress: Float? = null,
	cardWidth: Dp = 280.dp,
	onFocusChanged: ((Boolean) -> Unit)? = null,
) {
	PremiumCard(
		onClick = onClick,
		modifier = modifier.width(cardWidth),
		onFocusChanged = onFocusChanged,
	) { focused ->
		Box(
			modifier = Modifier.aspectRatio(16f / 9f),
		) {
			// Backdrop image
			CardImage(
				url = imageUrl,
				modifier = Modifier.fillMaxSize(),
			)

			// Bottom gradient for text readability
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(80.dp)
					.align(Alignment.BottomCenter)
					.background(
						Brush.verticalGradient(
							colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
						)
					),
			)

			// Title and subtitle (visible on focus)
			AnimatedVisibility(
				visible = focused,
				enter = fadeIn() + slideInVertically { it / 3 },
				exit = fadeOut() + slideOutVertically { it / 3 },
				modifier = Modifier
					.align(Alignment.BottomStart)
					.padding(horizontal = 12.dp, vertical = 8.dp),
			) {
				Column {
					Text(
						text = title,
						style = JellyfinTheme.typography.labelLarge,
						color = Color.White,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
					)
					if (subtitle != null) {
						Text(
							text = subtitle,
							style = JellyfinTheme.typography.labelSmall,
							color = Color.White.copy(alpha = 0.7f),
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
						)
					}
				}
			}

			// Progress bar
			if (progress != null && progress > 0f) {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(3.dp)
						.align(Alignment.BottomCenter),
				) {
					// Background
					Box(
						modifier = Modifier
							.fillMaxSize()
							.background(JellyfinTheme.colorScheme.progressBarBackground),
					)
					// Fill
					Box(
						modifier = Modifier
							.fillMaxWidth(progress.coerceIn(0f, 1f))
							.height(3.dp)
							.background(JellyfinTheme.colorScheme.progressBar),
					)
				}
			}
		}
	}
}
