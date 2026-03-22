package org.jellyfin.androidtv.ui.composable.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import org.jellyfin.androidtv.ui.base.CircularProgressIndicator
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.composable.card.CardImage
import org.jellyfin.androidtv.ui.composable.card.PosterCard
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class PremiumDetailFragment : Fragment() {
	companion object {
		const val ARG_ITEM_ID = "ItemId"
	}

	private val detailViewModel by viewModel<DetailOverlayViewModel>()
	private val navigationRepository by inject<NavigationRepository>()
	private val api by inject<ApiClient>()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	) = content {
		val itemId = arguments?.getString(ARG_ITEM_ID)?.toUUIDOrNull()

		LaunchedEffect(itemId) {
			if (itemId != null) detailViewModel.loadItem(itemId)
		}

		JellyfinTheme {
			val state by detailViewModel.state.collectAsState()

			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(JellyfinTheme.colorScheme.background),
			) {
				if (state.isLoading) {
					Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
						CircularProgressIndicator()
					}
				} else if (state.item != null) {
					DetailPage(
						state = state,
						api = api,
						viewModel = detailViewModel,
						onPlayClick = { item ->
							navigationRepository.navigate(Destinations.itemDetails(item.id))
						},
						onItemClick = { item ->
							navigationRepository.navigate(Destinations.premiumDetail(item.id))
						},
					)
				}
			}
		}
	}
}

@Composable
private fun DetailPage(
	state: DetailState,
	api: ApiClient,
	viewModel: DetailOverlayViewModel,
	onPlayClick: (BaseItemDto) -> Unit,
	onItemClick: (BaseItemDto) -> Unit,
) {
	val item = state.item ?: return
	val playButtonFocus = remember { FocusRequester() }

	LaunchedEffect(item.id) {
		playButtonFocus.requestFocus()
	}

	Column(modifier = Modifier.fillMaxSize()) {
		// ── TOP ROW: Backdrop left + Info right ──
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.weight(0.55f)
				.padding(start = 48.dp, end = 48.dp, top = 24.dp),
		) {
			// Backdrop thumbnail
			CardImage(
				url = item.itemBackdropImages.firstOrNull()?.getUrl(api),
				modifier = Modifier
					.fillMaxHeight()
					.aspectRatio(16f / 9f),
			)

			Spacer(modifier = Modifier.width(32.dp))

			// Info + buttons
			Column(
				modifier = Modifier
					.fillMaxHeight()
					.weight(1f),
				verticalArrangement = Arrangement.Center,
			) {
				Text(
					text = item.name.orEmpty(),
					style = JellyfinTheme.typography.displayMedium,
					color = JellyfinTheme.colorScheme.onBackground,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
				)

				Spacer(modifier = Modifier.height(6.dp))

				// Metadata
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
						color = JellyfinTheme.colorScheme.secondaryAccent,
					)
				}

				val genres = item.genres?.take(3)
				if (!genres.isNullOrEmpty()) {
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = genres.joinToString("  •  "),
						style = JellyfinTheme.typography.labelSmall,
						color = JellyfinTheme.colorScheme.primaryAccent,
					)
				}

				item.overview?.let { overview ->
					Spacer(modifier = Modifier.height(10.dp))
					Text(
						text = overview,
						style = JellyfinTheme.typography.bodyMedium,
						color = JellyfinTheme.colorScheme.onBackground.copy(alpha = 0.7f),
						maxLines = 3,
						overflow = TextOverflow.Ellipsis,
					)
				}

				Spacer(modifier = Modifier.height(16.dp))

				// Action buttons
				Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					val hasProgress = item.userData?.playbackPositionTicks?.let { it > 0 } == true

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
				}
			}
		}

		// ── BOTTOM ROW: Related content ──
		if (state.similarItems.isNotEmpty()) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.weight(0.45f)
					.padding(top = 16.dp),
			) {
				Text(
					text = "More Like This",
					style = JellyfinTheme.typography.titleMedium,
					color = JellyfinTheme.colorScheme.onBackground,
					modifier = Modifier.padding(horizontal = 48.dp),
				)

				Spacer(modifier = Modifier.height(8.dp))

				Row(
					horizontalArrangement = Arrangement.spacedBy(12.dp),
					modifier = Modifier
						.padding(horizontal = 48.dp)
						.fillMaxWidth(),
				) {
					state.similarItems.take(7).forEach { similarItem ->
						val imageUrl = similarItem.itemImages[ImageType.PRIMARY]?.getUrl(api)
						PosterCard(
							imageUrl = imageUrl,
							title = similarItem.name.orEmpty(),
							subtitle = similarItem.productionYear?.toString(),
							cardWidth = 110.dp,
							onClick = { onItemClick(similarItem) },
						)
					}
				}
			}
		}
	}
}
