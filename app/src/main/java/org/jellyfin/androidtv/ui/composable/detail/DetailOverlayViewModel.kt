package org.jellyfin.androidtv.ui.composable.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber
import java.util.UUID

data class DetailState(
	val item: BaseItemDto? = null,
	val seasons: List<BaseItemDto> = emptyList(),
	val episodes: List<BaseItemDto> = emptyList(),
	val similarItems: List<BaseItemDto> = emptyList(),
	val isLoading: Boolean = true,
)

class DetailOverlayViewModel(
	private val api: ApiClient,
	private val itemMutationRepository: ItemMutationRepository,
) : ViewModel() {
	private val _state = MutableStateFlow(DetailState())
	val state: StateFlow<DetailState> = _state.asStateFlow()

	fun loadItem(itemId: UUID) {
		_state.value = DetailState(isLoading = true)

		viewModelScope.launch(Dispatchers.IO) {
			try {
				val item by api.userLibraryApi.getItem(
					itemId = itemId,
				)

				_state.value = _state.value.copy(item = item, isLoading = false)

				// Load type-specific data in parallel
				when (item.type) {
					BaseItemKind.SERIES -> loadSeriesData(item)
					BaseItemKind.MOVIE -> loadSimilarItems(itemId)
					BaseItemKind.EPISODE -> loadEpisodeSiblings(item)
					else -> loadSimilarItems(itemId)
				}
			} catch (e: Exception) {
				Timber.e(e, "Failed to load item details")
				_state.value = _state.value.copy(isLoading = false)
			}
		}
	}

	private suspend fun loadSeriesData(series: BaseItemDto) {
		try {
			// Load seasons
			val seasonsResult by api.tvShowsApi.getSeasons(
				seriesId = series.id,
				fields = ItemRepository.itemFields,
			)
			val seasons = seasonsResult.items.orEmpty()
			_state.value = _state.value.copy(seasons = seasons)

			// Load episodes for first season
			if (seasons.isNotEmpty()) {
				loadEpisodesForSeason(series.id, seasons.first().id)
			}

			// Load similar
			loadSimilarItems(series.id)
		} catch (e: Exception) {
			Timber.w(e, "Failed to load series data")
		}
	}

	fun loadEpisodesForSeason(seriesId: UUID, seasonId: UUID) {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val result by api.tvShowsApi.getEpisodes(
					seriesId = seriesId,
					seasonId = seasonId,
					fields = ItemRepository.itemFields,
				)
				_state.value = _state.value.copy(episodes = result.items.orEmpty())
			} catch (e: Exception) {
				Timber.w(e, "Failed to load episodes")
			}
		}
	}

	private suspend fun loadEpisodeSiblings(episode: BaseItemDto) {
		val seriesId = episode.seriesId ?: return
		val seasonId = episode.seasonId ?: return
		try {
			val result by api.tvShowsApi.getEpisodes(
				seriesId = seriesId,
				seasonId = seasonId,
				fields = ItemRepository.itemFields,
			)
			_state.value = _state.value.copy(episodes = result.items.orEmpty())
			loadSimilarItems(seriesId)
		} catch (e: Exception) {
			Timber.w(e, "Failed to load episode siblings")
		}
	}

	private suspend fun loadSimilarItems(itemId: UUID) {
		try {
			val result by api.libraryApi.getSimilarItems(
				itemId = itemId,
				limit = 12,
				fields = ItemRepository.itemFields,
			)
			_state.value = _state.value.copy(similarItems = result.items.orEmpty())
		} catch (e: Exception) {
			Timber.w(e, "Failed to load similar items")
		}
	}

	fun toggleFavorite() {
		val item = _state.value.item ?: return
		viewModelScope.launch {
			val isFavorite = item.userData?.isFavorite == true
			itemMutationRepository.setFavorite(item.id, !isFavorite)
			// Reload to get updated state
			loadItem(item.id)
		}
	}

	fun togglePlayed() {
		val item = _state.value.item ?: return
		viewModelScope.launch {
			val isPlayed = item.userData?.played == true
			itemMutationRepository.setPlayed(item.id, !isPlayed)
			loadItem(item.id)
		}
	}
}
