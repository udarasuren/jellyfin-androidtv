package org.jellyfin.androidtv.ui.composable.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.CircularProgressIndicator
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.composable.card.CardImage
import org.jellyfin.androidtv.ui.composable.card.PosterCard
import org.jellyfin.androidtv.ui.composable.row.ContentRow
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import java.util.UUID

@Composable
fun DetailOverlay(
	visible: Boolean,
	itemId: UUID?,
	viewModel: DetailOverlayViewModel,
	api: ApiClient,
	onDismiss: () -> Unit,
	onPlayClick: (BaseItemDto) -> Unit,
	onItemClick: (BaseItemDto) -> Unit,
) {
	LaunchedEffect(itemId) {
		if (itemId != null) viewModel.loadItem(itemId)
	}

	AnimatedVisibility(
		visible = visible && itemId != null,
		enter = fadeIn(tween(300)) + slideInVertically(tween(400)) { it / 3 },
		exit = fadeOut(tween(200)) + slideOutVertically(tween(300)) { it / 3 },
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(JellyfinTheme.colorScheme.background.copy(alpha = 0.95f)),
		) {
			val state by viewModel.state.collectAsState()

			if (state.isLoading) {
				Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
					CircularProgressIndicator()
				}
			} else if (state.item != null) {
				DetailContent(
					state = state,
					api = api,
					viewModel = viewModel,
					onPlayClick = onPlayClick,
					onItemClick = onItemClick,
					onDismiss = onDismiss,
				)
			}
		}
	}
}

@Composable
private fun DetailContent(
	state: DetailState,
	api: ApiClient,
	viewModel: DetailOverlayViewModel,
	onPlayClick: (BaseItemDto) -> Unit,
	onItemClick: (BaseItemDto) -> Unit,
	onDismiss: () -> Unit,
) {
	val item = state.item ?: return
	var selectedSeasonId by remember(state.seasons) {
		mutableStateOf(state.seasons.firstOrNull()?.id)
	}

	LazyColumn {
		// Hero backdrop
		item(key = "backdrop") {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.aspectRatio(21f / 9f),
			) {
				val backdropUrl = item.itemBackdropImages.firstOrNull()?.getUrl(api)
				CardImage(
					url = backdropUrl,
					modifier = Modifier.fillMaxSize(),
				)

				// Gradient scrim
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(200.dp)
						.align(Alignment.BottomCenter)
						.background(
							Brush.verticalGradient(
								colors = listOf(
									Color.Transparent,
									JellyfinTheme.colorScheme.background.copy(alpha = 0.95f),
								),
							)
						),
				)
			}
		}

		// Metadata section
		item(key = "metadata") {
			Column(
				modifier = Modifier.padding(horizontal = 48.dp),
			) {
				// Title
				Text(
					text = item.name.orEmpty(),
					style = JellyfinTheme.typography.displayMedium,
					color = JellyfinTheme.colorScheme.onBackground,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
				)

				Spacer(modifier = Modifier.height(8.dp))

				// Metadata line
				val metaItems = buildList {
					item.productionYear?.let { add(it.toString()) }
					item.officialRating?.let { add(it) }
					item.runTimeTicks?.let { ticks ->
						val minutes = ticks / 600_000_000
						if (minutes > 0) add("${minutes}m")
					}
					item.communityRating?.let { add("★ %.1f".format(it)) }
				}
				if (metaItems.isNotEmpty()) {
					Text(
						text = metaItems.joinToString(" • "),
						style = JellyfinTheme.typography.bodyMedium,
						color = JellyfinTheme.colorScheme.secondaryAccent,
					)
				}

				// Genres
				val genres = item.genres?.take(4)
				if (!genres.isNullOrEmpty()) {
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = genres.joinToString(" • "),
						style = JellyfinTheme.typography.labelSmall,
						color = JellyfinTheme.colorScheme.primaryAccent,
					)
				}

				Spacer(modifier = Modifier.height(12.dp))

				// Synopsis
				item.overview?.let { overview ->
					Text(
						text = overview,
						style = JellyfinTheme.typography.bodyMedium,
						color = JellyfinTheme.colorScheme.onBackground.copy(alpha = 0.8f),
						maxLines = 4,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.fillMaxWidth(0.65f),
					)
				}

				Spacer(modifier = Modifier.height(20.dp))

				// Action buttons
				Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					val hasProgress = item.userData?.playbackPositionTicks?.let { it > 0 } == true

					Button(onClick = { onPlayClick(item) }) {
						Text(
							text = if (hasProgress) "▶  Resume" else "▶  Play",
							style = JellyfinTheme.typography.labelLarge,
						)
					}

					Button(onClick = { viewModel.toggleFavorite() }) {
						val isFav = item.userData?.isFavorite == true
						Text(
							text = if (isFav) "♥  Favorited" else "♡  Favorite",
							style = JellyfinTheme.typography.labelLarge,
						)
					}

					Button(onClick = { viewModel.togglePlayed() }) {
						val isPlayed = item.userData?.played == true
						Text(
							text = if (isPlayed) "✓  Watched" else "Mark Watched",
							style = JellyfinTheme.typography.labelLarge,
						)
					}

					Button(onClick = onDismiss) {
						Text(text = "Close", style = JellyfinTheme.typography.labelLarge)
					}
				}

				Spacer(modifier = Modifier.height(24.dp))
			}
		}

		// Episodes (for series)
		if (item.type == BaseItemKind.SERIES && state.seasons.isNotEmpty()) {
			item(key = "episodes") {
				Column(modifier = Modifier.padding(bottom = 24.dp)) {
					Text(
						text = "Episodes",
						style = JellyfinTheme.typography.titleMedium,
						color = JellyfinTheme.colorScheme.onBackground,
						modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
					)

					EpisodeSelector(
						seasons = state.seasons,
						episodes = state.episodes,
						api = api,
						selectedSeasonId = selectedSeasonId,
						onSeasonSelected = { season ->
							selectedSeasonId = season.id
							viewModel.loadEpisodesForSeason(item.id, season.id)
						},
						onEpisodeClick = onItemClick,
					)
				}
			}
		}

		// Similar items
		if (state.similarItems.isNotEmpty()) {
			item(key = "similar") {
				ContentRow(
					title = "More Like This",
					items = state.similarItems,
					modifier = Modifier.padding(bottom = 48.dp),
				) { _, similarItem ->
					val imageUrl = similarItem.itemImages[ImageType.PRIMARY]?.getUrl(api)
					PosterCard(
						imageUrl = imageUrl,
						title = similarItem.name.orEmpty(),
						subtitle = similarItem.productionYear?.toString(),
						onClick = {
							viewModel.loadItem(similarItem.id)
						},
					)
				}
			}
		}
	}
}
