package org.jellyfin.androidtv.ui.composable.row

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.modifier.fadingEdges

/**
 * A horizontal content row with a title header and scrollable items.
 * Uses LazyRow for efficient lazy rendering with D-pad navigation support.
 */
@Composable
fun <T> ContentRow(
	title: String,
	items: List<T>,
	modifier: Modifier = Modifier,
	keyProvider: ((T) -> Any)? = null,
	itemContent: @Composable (index: Int, item: T) -> Unit,
) {
	if (items.isEmpty()) return

	Column(modifier = modifier.fillMaxWidth()) {
		// Row header
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 48.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			Text(
				text = title,
				style = JellyfinTheme.typography.titleMedium,
				color = JellyfinTheme.colorScheme.onBackground,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
		}

		Spacer(modifier = Modifier.height(8.dp))

		// Scrollable items row with fading edges
		LazyRow(
			contentPadding = PaddingValues(horizontal = 44.dp),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			modifier = Modifier.fadingEdges(horizontal = 32.dp),
		) {
			itemsIndexed(
				items = items,
				key = if (keyProvider != null) { index, item -> keyProvider(item) } else null,
			) { index, item ->
				itemContent(index, item)
			}
		}
	}
}
