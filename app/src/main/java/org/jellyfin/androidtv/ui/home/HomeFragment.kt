package org.jellyfin.androidtv.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.composable.detail.DetailOverlay
import org.jellyfin.androidtv.ui.composable.detail.DetailOverlayViewModel
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.UUID

class HomeFragment : Fragment() {
	private val sessionRepository by inject<SessionRepository>()
	private val serverRepository by inject<ServerRepository>()
	private val notificationRepository by inject<NotificationsRepository>()
	private val navigationRepository by inject<NavigationRepository>()
	private val itemLauncher by inject<ItemLauncher>()
	private val homeViewModel by viewModel<HomeViewModel>()
	private val detailViewModel by viewModel<DetailOverlayViewModel>()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		val api = inject<ApiClient>().value

		var detailItemId by remember { mutableStateOf<UUID?>(null) }
		var showDetail by remember { mutableStateOf(false) }

		// Handle back press when detail overlay is visible
		BackHandler(enabled = showDetail) {
			showDetail = false
			detailItemId = null
		}

		JellyfinTheme {
			Box(modifier = Modifier.fillMaxSize()) {
				Column(modifier = Modifier.fillMaxSize()) {
					MainToolbar(MainToolbarActiveButton.Home)

					HomeScreen(
						viewModel = homeViewModel,
						onItemClick = { item ->
							when (item.type) {
								// Libraries browse directly
								BaseItemKind.USER_VIEW,
								BaseItemKind.COLLECTION_FOLDER -> navigateToItem(item)
								// Everything else opens the detail overlay
								else -> {
									detailItemId = item.id
									showDetail = true
								}
							}
						},
						onPlayClick = { item -> navigateToItem(item) },
					)
				}

				// Detail overlay on top of everything
				DetailOverlay(
					visible = showDetail,
					itemId = detailItemId,
					viewModel = detailViewModel,
					api = api,
					onDismiss = {
						showDetail = false
						detailItemId = null
					},
					onPlayClick = { item ->
						showDetail = false
						detailItemId = null
						navigateToItem(item)
					},
					onItemClick = { item ->
						// Navigate within the overlay for episodes/similar
						detailItemId = item.id
					},
				)
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		sessionRepository.currentSession
			.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
			.map { session ->
				if (session == null) null
				else serverRepository.getServer(session.serverId)
			}
			.onEach { server ->
				notificationRepository.updateServerNotifications(server)
			}
			.launchIn(viewLifecycleOwner.lifecycleScope)
	}

	override fun onResume() {
		super.onResume()
		homeViewModel.refresh()
	}

	private fun navigateToItem(item: BaseItemDto) {
		when (item.type) {
			BaseItemKind.USER_VIEW,
			BaseItemKind.COLLECTION_FOLDER -> itemLauncher.launchUserView(item)
			BaseItemKind.SERIES,
			BaseItemKind.MUSIC_ARTIST -> navigationRepository.navigate(Destinations.itemDetails(item.id))
			BaseItemKind.MUSIC_ALBUM,
			BaseItemKind.PLAYLIST -> navigationRepository.navigate(Destinations.itemList(item.id))
			else -> navigationRepository.navigate(Destinations.itemDetails(item.id))
		}
	}
}
