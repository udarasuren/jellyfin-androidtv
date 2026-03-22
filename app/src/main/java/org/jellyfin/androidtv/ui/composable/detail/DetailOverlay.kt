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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
				.background(JellyfinTheme.colorScheme.background),
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
	val playButtonFocus = remember { FocusRequester() }
	val listState = rememberLazyListState()

	// Auto-focus the Play button when content loads
	LaunchedEffect(item.id) {
		listState.scrollToItem(0)
		playButtonFocus.requestFocus()
	}

	LazyColumn(
		state = listState,
		modifier = Modifier.fillMaxSize(),
	) {
		// Item 0: Backdrop with info text (NOT buttons)
		item(key = "backdrop_${item.id}") {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(380.dp),
			) {
				val backdropUrl = item.itemBackdropImages.firstOrNull()?.getUrl(api)
				CardImage(url = backdropUrl, modifier = Modifier.fillMaxSize())

				// Left gradient
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(
							Brush.horizontalGradient(
								colors = listOf(
									JellyfinTheme.colorScheme.background.copy(alpha = 0.9f),
									JellyfinTheme.colorScheme.background.copy(alpha = 0.4f),
									Color.Transparent,
								),
							)
						),
				)
				// Bottom gradient
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(120.dp)
						.align(Alignment.BottomCenter)
						.background(
							Brush.verticalGradient(
								colors = listOf(Color.Transparent, JellyfinTheme.colorScheme.background),
							)
						),
				)

				// Info text on the left
				Column(
					modifier = Modifier
						.align(Alignment.BottomStart)
						.padding(start = 48.dp, bottom = 8.dp, end = 48.dp)
						.fillMaxWidth(0.55f),
				) {
					Text(
						text = item.name.orEmpty(),
						style = JellyfinTheme.typography.displayLarge,
						color = Color.White,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
					)

					Spacer(modifier = Modifier.height(8.dp))

					val metaItems = buildList {
						item.productionYear?.let { add(it.toString()) }
						item.officialRating?.let { add(it) }
						item.runTimeTicks?.let { ticks ->
							val hours = ticks / 36_000_000_000
							val minutes = (ticks % 36_000_000_000) / 600_000_000
							if (hours > 0) add("${hours}h ${minutes}m")
							else if (minutes > 0) add("${minutes}m")
						}
						item.communityRating?.let { add("★ %.1f".format(it)) }
					}
					if (metaItems.isNotEmpty()) {
						Text(
							text = metaItems.joinToString("  •  "),
							style = JellyfinTheme.typography.bodyMedium,
							color = Color.White.copy(alpha = 0.7f),
						)
					}

					val genres = item.genres?.take(4)
					if (!genres.isNullOrEmpty()) {
						Spacer(modifier = Modifier.height(4.dp))
						Text(
							text = genres.joinToString("  •  "),
							style = JellyfinTheme.typography.labelSmall,
							color = JellyfinTheme.colorScheme.primaryAccent,
						)
					}

					item.overview?.let { overview ->
						Spacer(modifier = Modifier.height(12.dp))
						Text(
							text = overview,
							style = JellyfinTheme.typography.bodyMedium,
							color = Color.White.copy(alpha = 0.8f),
							maxLines = 3,
							overflow = TextOverflow.Ellipsis,
						)
					}
				}
			}
		}

		// Item 1: Action buttons — separate LazyColumn item so focus works naturally
		item(key = "buttons_${item.id}") {
			val hasProgress = item.userData?.playbackPositionTicks?.let { it > 0 } == true

			Row(
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp),
			) {
				Button(
					onClick = { onPlayClick(item) },
					modifier = Modifier.focusRequester(playButtonFocus),
				) {
					Text(
						text = if (hasProgress) "\u25B6  Resume" else "\u25B6  Play",
						style = JellyfinTheme.typography.labelLarge,
					)
				}

				Button(onClick = { viewModel.toggleFavorite() }) {
					val isFav = item.userData?.isFavorite == true
					Text(
						text = if (isFav) "\u2665  Favorited" else "\u2661  Favorite",
						style = JellyfinTheme.typography.labelLarge,
					)
				}

				Button(onClick = { viewModel.togglePlayed() }) {
					val isPlayed = item.userData?.played == true
					Text(
						text = if (isPlayed) "\u2713  Watched" else "Mark Watched",
						style = JellyfinTheme.typography.labelLarge,
					)
				}

				Button(onClick = onDismiss) {
					Text(text = "Close", style = JellyfinTheme.typography.labelLarge)
				}
			}
		}

		// Item 2: Episodes (for series)
		if (item.type == BaseItemKind.SERIES && state.seasons.isNotEmpty()) {
			item(key = "episodes_${item.id}") {
				Column(modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
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

		// Item 3: Similar items
		if (state.similarItems.isNotEmpty()) {
			item(key = "similar_${item.id}") {
				ContentRow(
					title = "More Like This",
					items = state.similarItems,
				) { _, similarItem ->
					val imageUrl = similarItem.itemImages[ImageType.PRIMARY]?.getUrl(api)
					PosterCard(
						imageUrl = imageUrl,
						title = similarItem.name.orEmpty(),
						subtitle = similarItem.productionYear?.toString(),
						onClick = { viewModel.loadItem(similarItem.id) },
					)
				}
			}
		}

		// Bottom padding
		item { Spacer(modifier = Modifier.height(48.dp)) }
	}
}
