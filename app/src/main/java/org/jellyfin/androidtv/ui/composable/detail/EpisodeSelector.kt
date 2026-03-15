package org.jellyfin.androidtv.ui.composable.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.card.CardImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType

@Composable
fun EpisodeSelector(
	seasons: List<BaseItemDto>,
	episodes: List<BaseItemDto>,
	api: ApiClient,
	selectedSeasonId: java.util.UUID?,
	onSeasonSelected: (BaseItemDto) -> Unit,
	onEpisodeClick: (BaseItemDto) -> Unit,
) {
	Column(modifier = Modifier.fillMaxWidth()) {
		// Season tabs
		if (seasons.size > 1) {
			LazyRow(
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				contentPadding = PaddingValues(horizontal = 32.dp),
			) {
				items(seasons, key = { it.id }) { season ->
					val isSelected = season.id == selectedSeasonId
					SeasonTab(
						name = season.name.orEmpty(),
						isSelected = isSelected,
						onClick = { onSeasonSelected(season) },
					)
				}
			}
			Spacer(modifier = Modifier.height(16.dp))
		}

		// Episode cards
		LazyRow(
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			contentPadding = PaddingValues(horizontal = 32.dp),
		) {
			items(episodes) { episode ->
				EpisodeCard(
					episode = episode,
					api = api,
					onClick = { onEpisodeClick(episode) },
				)
			}
		}
	}
}

@Composable
private fun SeasonTab(
	name: String,
	isSelected: Boolean,
	onClick: () -> Unit,
) {
	var focused by remember { mutableStateOf(false) }
	val bgColor = when {
		isSelected -> JellyfinTheme.colorScheme.primaryAccent
		focused -> JellyfinTheme.colorScheme.surfaceContainerHighest
		else -> Color.Transparent
	}
	val textColor = when {
		isSelected -> JellyfinTheme.colorScheme.onPrimaryAccent
		else -> JellyfinTheme.colorScheme.onBackground
	}

	Box(
		modifier = Modifier
			.clip(RoundedCornerShape(20.dp))
			.background(bgColor)
			.onFocusChanged { focused = it.isFocused }
			.focusable()
			.clickable { onClick() }
			.padding(horizontal = 16.dp, vertical = 8.dp),
	) {
		Text(
			text = name,
			style = JellyfinTheme.typography.labelLarge,
			color = textColor,
		)
	}
}

@Composable
private fun EpisodeCard(
	episode: BaseItemDto,
	api: ApiClient,
	onClick: () -> Unit,
) {
	var focused by remember { mutableStateOf(false) }
	val bgColor = if (focused) JellyfinTheme.colorScheme.surfaceContainerHighest
	else JellyfinTheme.colorScheme.surfaceContainerHigh

	Column(
		modifier = Modifier
			.width(240.dp)
			.clip(RoundedCornerShape(8.dp))
			.background(bgColor)
			.onFocusChanged { focused = it.isFocused }
			.focusable()
			.clickable { onClick() },
	) {
		// Thumbnail
		val imageUrl = episode.itemBackdropImages.firstOrNull()?.getUrl(api)
			?: episode.itemImages[ImageType.PRIMARY]?.getUrl(api)

		CardImage(
			url = imageUrl,
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(16f / 9f),
		)

		// Episode info
		Column(modifier = Modifier.padding(10.dp)) {
			Text(
				text = "E${episode.indexNumber} • ${episode.name.orEmpty()}",
				style = JellyfinTheme.typography.labelLarge,
				color = JellyfinTheme.colorScheme.onBackground,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)

			val runtime = episode.runTimeTicks?.let { ticks ->
				val minutes = ticks / 600_000_000
				"${minutes}m"
			}
			if (runtime != null) {
				Text(
					text = runtime,
					style = JellyfinTheme.typography.labelSmall,
					color = JellyfinTheme.colorScheme.secondaryAccent,
				)
			}

			episode.overview?.let { overview ->
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = overview,
					style = JellyfinTheme.typography.labelSmall,
					color = JellyfinTheme.colorScheme.secondaryAccent.copy(alpha = 0.7f),
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
				)
			}
		}
	}
}
