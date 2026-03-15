package org.jellyfin.androidtv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

enum class HomeRowType {
	ContinueWatching,
	NextUp,
	RecentlyAdded,
	Library,
	LiveTV,
}

data class HomeRow(
	val title: String,
	val items: List<BaseItemDto>,
	val rowType: HomeRowType,
)

sealed class HomeScreenState {
	data object Loading : HomeScreenState()
	data class Loaded(
		val heroItems: List<BaseItemDto>,
		val rows: List<HomeRow>,
	) : HomeScreenState()

	data class Error(val message: String) : HomeScreenState()
}

class HomeViewModel(
	private val api: ApiClient,
	private val userRepository: UserRepository,
	private val userViewsRepository: UserViewsRepository,
	private val userSettingPreferences: UserSettingPreferences,
) : ViewModel() {
	private val _state = MutableStateFlow<HomeScreenState>(HomeScreenState.Loading)
	val state: StateFlow<HomeScreenState> = _state.asStateFlow()

	init {
		loadHomeScreen()
	}

	fun refresh() {
		loadHomeScreen()
	}

	private fun loadHomeScreen() {
		_state.value = HomeScreenState.Loading

		viewModelScope.launch(Dispatchers.IO) {
			try {
				val currentUser = withTimeout(30.seconds) {
					userRepository.currentUser.filterNotNull().first()
				}

				val rows = mutableListOf<HomeRow>()
				val heroItems = mutableListOf<BaseItemDto>()

				// Load hero items — recently added movies/shows with backdrop images
				try {
					val recentItems by api.userLibraryApi.getLatestMedia(
						limit = 10,
						imageTypeLimit = 1,
						enableImageTypes = listOf(ImageType.BACKDROP, ImageType.PRIMARY, ImageType.LOGO),
						includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
					)
					heroItems.addAll(
						recentItems.filter { item ->
							item.backdropImageTags?.isNotEmpty() == true
						}.take(5)
					)
				} catch (e: Exception) {
					Timber.w(e, "Failed to load hero items")
				}

				// Load sections based on user preferences
				val homeSections = userSettingPreferences.activeHomesections

				for (section in homeSections) {
					try {
						when (section) {
							HomeSectionType.RESUME -> loadContinueWatching()?.let { rows.add(it) }
							HomeSectionType.NEXT_UP -> loadNextUp()?.let { rows.add(it) }
							HomeSectionType.LATEST_MEDIA -> {
								val views = userViewsRepository.views.first()
								loadRecentlyAddedRows(views).forEach { rows.add(it) }
							}
							HomeSectionType.LIBRARY_TILES_SMALL,
							HomeSectionType.LIBRARY_BUTTONS -> loadLibraryRow()?.let { rows.add(it) }
							else -> Unit
						}
					} catch (e: Exception) {
						Timber.w(e, "Failed to load home section: %s", section)
					}
				}

				_state.value = HomeScreenState.Loaded(
					heroItems = heroItems,
					rows = rows,
				)
			} catch (e: Exception) {
				Timber.e(e, "Failed to load home screen")
				_state.value = HomeScreenState.Error(e.message ?: "Unknown error")
			}
		}
	}

	private suspend fun loadContinueWatching(): HomeRow? {
		val result by api.itemsApi.getResumeItems(
			limit = 12,
			fields = ItemRepository.itemFields,
			imageTypeLimit = 1,
			enableTotalRecordCount = false,
			mediaTypes = listOf(MediaType.VIDEO),
			excludeItemTypes = listOf(BaseItemKind.AUDIO_BOOK),
		)
		val items = result.items.orEmpty()
		if (items.isEmpty()) return null
		return HomeRow(
			title = "Continue Watching",
			items = items,
			rowType = HomeRowType.ContinueWatching,
		)
	}

	private suspend fun loadNextUp(): HomeRow? {
		val result by api.tvShowsApi.getNextUp(
			imageTypeLimit = 1,
			limit = 12,
			enableResumable = false,
			fields = ItemRepository.itemFields,
		)
		val items = result.items.orEmpty()
		if (items.isEmpty()) return null
		return HomeRow(
			title = "Next Up",
			items = items,
			rowType = HomeRowType.NextUp,
		)
	}

	private suspend fun loadRecentlyAddedRows(views: Collection<BaseItemDto>): List<HomeRow> {
		val rows = mutableListOf<HomeRow>()
		for (view in views) {
			val skipTypes = setOf("playlists", "livetv", "boxsets", "books")
			if (view.collectionType?.serialName in skipTypes) continue

			try {
				val items by api.userLibraryApi.getLatestMedia(
					parentId = view.id,
					limit = 16,
					imageTypeLimit = 1,
					fields = ItemRepository.itemFields,
				)
				if (items.isNotEmpty()) {
					rows.add(
						HomeRow(
							title = "Recently Added in ${view.name}",
							items = items,
							rowType = HomeRowType.RecentlyAdded,
						)
					)
				}
			} catch (e: Exception) {
				Timber.w(e, "Failed to load recently added for %s", view.name)
			}
		}
		return rows
	}

	private suspend fun loadLibraryRow(): HomeRow? {
		val views = userViewsRepository.views.first()
		if (views.isEmpty()) return null
		return HomeRow(
			title = "My Media",
			items = views.toList(),
			rowType = HomeRowType.Library,
		)
	}
}
