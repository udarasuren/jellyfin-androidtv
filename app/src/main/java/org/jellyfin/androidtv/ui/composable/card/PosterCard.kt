package org.jellyfin.androidtv.ui.composable.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text

/**
 * 2:3 poster card for movies and series.
 * Shows title below the card, subtitle appears on focus.
 */
@Composable
fun PosterCard(
	imageUrl: String?,
	title: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	subtitle: String? = null,
	cardWidth: Dp = 150.dp,
	onFocusChanged: ((Boolean) -> Unit)? = null,
) {
	Column(modifier = modifier.width(cardWidth)) {
		PremiumCard(
			onClick = onClick,
			onFocusChanged = onFocusChanged,
		) { focused ->
			Box(
				modifier = Modifier.aspectRatio(2f / 3f),
			) {
				CardImage(
					url = imageUrl,
					modifier = Modifier.fillMaxSize(),
				)
			}
		}

		// Title below card (always visible)
		Spacer(modifier = Modifier.height(6.dp))
		Text(
			text = title,
			style = JellyfinTheme.typography.labelLarge,
			color = JellyfinTheme.colorScheme.onBackground,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.padding(horizontal = 4.dp),
		)

		// Subtitle (visible on focus — managed by parent recomposition)
		if (subtitle != null) {
			Text(
				text = subtitle,
				style = JellyfinTheme.typography.labelSmall,
				color = JellyfinTheme.colorScheme.secondaryAccent,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.padding(horizontal = 4.dp),
			)
		}
	}
}
