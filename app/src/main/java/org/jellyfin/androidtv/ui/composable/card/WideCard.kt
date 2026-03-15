package org.jellyfin.androidtv.ui.composable.card

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
 * Extra-wide landscape card for "Because you watched" recommendations
 * and featured content rows. Wider than LandscapeCard with always-visible title.
 */
@Composable
fun WideCard(
	imageUrl: String?,
	title: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	subtitle: String? = null,
	cardWidth: Dp = 360.dp,
	onFocusChanged: ((Boolean) -> Unit)? = null,
) {
	PremiumCard(
		onClick = onClick,
		modifier = modifier.width(cardWidth),
		onFocusChanged = onFocusChanged,
	) { focused ->
		Box(
			modifier = Modifier.aspectRatio(21f / 9f),
		) {
			// Backdrop image
			CardImage(
				url = imageUrl,
				modifier = Modifier.fillMaxSize(),
			)

			// Bottom gradient
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(60.dp)
					.align(Alignment.BottomCenter)
					.background(
						Brush.verticalGradient(
							colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
						)
					),
			)

			// Title always visible
			Column(
				modifier = Modifier
					.align(Alignment.BottomStart)
					.padding(horizontal = 14.dp, vertical = 10.dp),
			) {
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
	}
}
