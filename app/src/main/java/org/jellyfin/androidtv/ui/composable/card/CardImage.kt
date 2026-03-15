package org.jellyfin.androidtv.ui.composable.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.jellyfin.androidtv.ui.base.JellyfinTheme

/**
 * Compose-native image component for use inside cards and lazy lists.
 * Uses Coil's Compose AsyncImage which handles lazy list recycling correctly,
 * unlike the legacy AndroidView-based AsyncImage.
 */
@Composable
fun CardImage(
	url: String?,
	modifier: Modifier = Modifier,
	contentScale: ContentScale = ContentScale.Crop,
) {
	if (url != null) {
		AsyncImage(
			model = url,
			contentDescription = null,
			contentScale = contentScale,
			modifier = modifier,
		)
	} else {
		Box(
			modifier = modifier
				.background(JellyfinTheme.colorScheme.surfaceContainerHigh),
		)
	}
}
