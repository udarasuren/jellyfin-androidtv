package org.jellyfin.androidtv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.CircularProgressIndicator
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.billboard.HeroBillboard
import org.jellyfin.androidtv.ui.composable.card.LandscapeCard
import org.jellyfin.androidtv.ui.composable.card.PosterCard
import org.jellyfin.androidtv.ui.composable.card.WideCard
import org.jellyfin.androidtv.ui.composable.row.ContentRow
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
	viewModel: HomeViewModel,
	onItemClick: (BaseItemDto) -> Unit = {},
	onPlayClick: (BaseItemDto) -> Unit = {},
) {
	val api = koinInject<ApiClient>()
	val state by viewModel.state.collectAsState()

	when (val currentState = state) {
		is HomeScreenState.Loading -> {
			Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				CircularProgressIndicator()
			}
		}

		is HomeScreenState.Error -> {
			Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				Text(
					text = "Unable to load home screen",
					style = JellyfinTheme.typography.bodyLarge,
					color = JellyfinTheme.colorScheme.onBackground,
				)
			}
		}

		is HomeScreenState.Loaded -> {
			HomeScreenContent(
				state = currentState,
				api = api,
				onItemClick = onItemClick,
				onPlayClick = onPlayClick,
			)
		}
	}
}

@Composable
private fun HomeScreenContent(
	state: HomeScreenState.Loaded,
	api: ApiClient,
	onItemClick: (BaseItemDto) -> Unit,
	onPlayClick: (BaseItemDto) -> Unit,
) {
	val listState = rememberLazyListState()
	val coroutineScope = rememberCoroutineScope()

	LazyColumn(
		state = listState,
		verticalArrangement = Arrangement.spacedBy(24.dp),
	) {
		// Hero billboard — uses fillMaxWidth + fixed height so LazyColumn
		// always reserves the correct space even during image reload
		if (state.heroItems.isNotEmpty()) {
			item(key = "hero") {
				Box(modifier = Modifier
					.fillMaxWidth()
					.height(400.dp)
				) {
					HeroBillboard(
						items = state.heroItems,
						backdropUrlProvider = { item ->
							item.itemBackdropImages.firstOrNull()?.getUrl(api)
						},
						onPlayClick = onPlayClick,
						onInfoClick = onItemClick,
						onFocusGained = {
							coroutineScope.launch {
								listState.animateScrollToItem(0)
							}
						},
						canExitUp = {
							listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 10
						},
						modifier = Modifier.fillMaxSize(),
					)
				}
			}
		}

		// Content rows
		items(state.rows, key = { it.title }) { row ->
			when (row.rowType) {
				HomeRowType.ContinueWatching -> ContinueWatchingRow(row, api, onItemClick)
				HomeRowType.NextUp -> NextUpRow(row, api, onItemClick)
				HomeRowType.RecentlyAdded -> RecentlyAddedRow(row, api, onItemClick)
				HomeRowType.Library -> LibraryRow(row, api, onItemClick)
				HomeRowType.LiveTV -> LiveTVRow(row, api, onItemClick)
			}
		}

		// Bottom padding
		item { Spacer(modifier = Modifier.height(48.dp)) }
	}
}

@Composable
private fun ContinueWatchingRow(
	row: HomeRow,
	api: ApiClient,
	onItemClick: (BaseItemDto) -> Unit,
) {
	ContentRow(title = row.title, items = row.items, keyProvider = { "${it.id}_${it.indexNumber}" }) { _, item ->
		val imageUrl = item.itemBackdropImages.firstOrNull()?.getUrl(api)
			?: item.itemImages[ImageType.PRIMARY]?.getUrl(api)
		val progress = item.userData?.playedPercentage?.let { it.toFloat() / 100f }
		val subtitle = when (item.type) {
			BaseItemKind.EPISODE -> "S${item.parentIndexNumber}:E${item.indexNumber} - ${item.seriesName.orEmpty()}"
			else -> item.productionYear?.toString()
		}

		LandscapeCard(
			imageUrl = imageUrl,
			title = item.name.orEmpty(),
			subtitle = subtitle,
			progress = progress,
			onClick = { onItemClick(item) },
		)
	}
}

@Composable
private fun NextUpRow(
	row: HomeRow,
	api: ApiClient,
	onItemClick: (BaseItemDto) -> Unit,
) {
	ContentRow(title = row.title, items = row.items, keyProvider = { "${it.id}_${it.indexNumber}" }) { _, item ->
		val imageUrl = item.itemBackdropImages.firstOrNull()?.getUrl(api)
			?: item.itemImages[ImageType.PRIMARY]?.getUrl(api)

		LandscapeCard(
			imageUrl = imageUrl,
			title = item.name.orEmpty(),
			subtitle = "S${item.parentIndexNumber}:E${item.indexNumber} - ${item.seriesName.orEmpty()}",
			onClick = { onItemClick(item) },
		)
	}
}

@Composable
private fun RecentlyAddedRow(
	row: HomeRow,
	api: ApiClient,
	onItemClick: (BaseItemDto) -> Unit,
) {
	ContentRow(title = row.title, items = row.items, keyProvider = { "${it.id}_${it.indexNumber}" }) { _, item ->
		val imageUrl = item.itemImages[ImageType.PRIMARY]?.getUrl(api)

		PosterCard(
			imageUrl = imageUrl,
			title = item.name.orEmpty(),
			subtitle = item.productionYear?.toString(),
			onClick = { onItemClick(item) },
		)
	}
}

@Composable
private fun LibraryRow(
	row: HomeRow,
	api: ApiClient,
	onItemClick: (BaseItemDto) -> Unit,
) {
	ContentRow(title = row.title, items = row.items, keyProvider = { "${it.id}_${it.indexNumber}" }) { _, item ->
		val imageUrl = item.itemImages[ImageType.PRIMARY]?.getUrl(api)
			?: item.itemBackdropImages.firstOrNull()?.getUrl(api)

		WideCard(
			imageUrl = imageUrl,
			title = item.name.orEmpty(),
			onClick = { onItemClick(item) },
		)
	}
}

@Composable
private fun LiveTVRow(
	row: HomeRow,
	api: ApiClient,
	onItemClick: (BaseItemDto) -> Unit,
) {
	ContentRow(title = row.title, items = row.items, keyProvider = { "${it.id}_${it.indexNumber}" }) { _, item ->
		val imageUrl = item.itemImages[ImageType.PRIMARY]?.getUrl(api)

		LandscapeCard(
			imageUrl = imageUrl,
			title = item.name.orEmpty(),
			onClick = { onItemClick(item) },
		)
	}
}
