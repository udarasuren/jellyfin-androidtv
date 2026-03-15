package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.UnableToConnectState
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.startup.StartupViewModel
import org.jellyfin.androidtv.util.getSummary
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SelectServerFragment : Fragment() {
	private val startupViewModel: StartupViewModel by activityViewModel()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		JellyfinTheme {
			SelectServerScreen(
				viewModel = startupViewModel,
				onServerClick = { server -> navigateToServer(server) },
				onDiscoveredServerClick = { server -> connectDiscoveredServer(server) },
				onManualEntry = {
					parentFragmentManager.commit {
						addToBackStack(null)
						replace<ServerAddFragment>(R.id.content_view)
					}
				},
				onDeleteServer = { server -> startupViewModel.deleteServer(server.id) },
			)
		}
	}

	private fun navigateToServer(server: Server) {
		requireActivity().supportFragmentManager.commit {
			replace<StartupToolbarFragment>(R.id.content_view)
			add<ServerFragment>(
				R.id.content_view, null,
				bundleOf(ServerFragment.ARG_SERVER_ID to server.id.toString()),
			)
			addToBackStack(null)
		}
	}

	private fun connectDiscoveredServer(server: Server) {
		startupViewModel.addServer(server.address).onEach { state ->
			if (state is ConnectedState) {
				parentFragmentManager.commit {
					replace<StartupToolbarFragment>(R.id.content_view)
					add<ServerFragment>(
						R.id.content_view, null,
						bundleOf(ServerFragment.ARG_SERVER_ID to state.id.toString()),
					)
				}
			} else if (state is UnableToConnectState) {
				Toast.makeText(
					requireContext(),
					getString(
						R.string.server_connection_failed_candidates,
						state.addressCandidates
							.map { "${it.key} ${it.value.getSummary(requireContext())}" }
							.joinToString(prefix = "\n", separator = "\n"),
					),
					Toast.LENGTH_LONG,
				).show()
			}
		}.launchIn(lifecycleScope)
	}

	override fun onResume() {
		super.onResume()
		startupViewModel.reloadStoredServers()
		startupViewModel.loadDiscoveryServers()
	}
}

@Composable
private fun SelectServerScreen(
	viewModel: StartupViewModel,
	onServerClick: (Server) -> Unit,
	onDiscoveredServerClick: (Server) -> Unit,
	onManualEntry: () -> Unit,
	onDeleteServer: (Server) -> Unit,
) {
	val storedServers by viewModel.storedServers.collectAsState(initial = emptyList())
	val discoveredServers by viewModel.discoveredServers.collectAsState(initial = emptyList())

	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier
				.fillMaxWidth(0.6f)
				.padding(vertical = 48.dp),
		) {
			// Title
			Text(
				text = "Connect to Server",
				style = JellyfinTheme.typography.displayMedium,
				color = JellyfinTheme.colorScheme.onBackground,
			)

			Spacer(modifier = Modifier.height(8.dp))

			Text(
				text = "Select a server or enter an address to get started",
				style = JellyfinTheme.typography.bodyMedium,
				color = JellyfinTheme.colorScheme.secondaryAccent,
			)

			Spacer(modifier = Modifier.height(32.dp))

			LazyColumn(
				verticalArrangement = Arrangement.spacedBy(8.dp),
				modifier = Modifier.weight(1f, fill = false),
			) {
				// Saved servers
				if (storedServers.isNotEmpty()) {
					item {
						Text(
							text = "Saved Servers",
							style = JellyfinTheme.typography.labelLarge,
							color = JellyfinTheme.colorScheme.secondaryAccent,
							modifier = Modifier.padding(bottom = 4.dp),
						)
					}
					items(storedServers, key = { it.id }) { server ->
						ServerCard(
							name = server.name,
							address = server.address,
							version = server.version,
							onClick = { onServerClick(server) },
						)
					}
				}

				// Discovered servers
				if (discoveredServers.isNotEmpty()) {
					item {
						Spacer(modifier = Modifier.height(16.dp))
						Text(
							text = "Discovered on Network",
							style = JellyfinTheme.typography.labelLarge,
							color = JellyfinTheme.colorScheme.secondaryAccent,
							modifier = Modifier.padding(bottom = 4.dp),
						)
					}
					items(discoveredServers, key = { "disc_${it.id}" }) { server ->
						ServerCard(
							name = server.name,
							address = server.address,
							version = server.version,
							onClick = { onDiscoveredServerClick(server) },
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(24.dp))

			// Manual entry button
			Button(onClick = onManualEntry) {
				Text(
					text = "Enter Server Address",
					style = JellyfinTheme.typography.labelLarge,
				)
			}

			// Version info
			Spacer(modifier = Modifier.height(16.dp))
			Text(
				text = "v${BuildConfig.VERSION_NAME}",
				style = JellyfinTheme.typography.labelSmall,
				color = JellyfinTheme.colorScheme.secondaryAccent.copy(alpha = 0.4f),
			)
		}
	}
}

@Composable
private fun ServerCard(
	name: String,
	address: String,
	version: String?,
	onClick: () -> Unit,
) {
	var focused by remember { mutableStateOf(false) }
	val scale by animateFloatAsState(
		targetValue = if (focused) 1.02f else 1f,
		animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
		label = "serverScale",
	)

	val bgColor = if (focused) JellyfinTheme.colorScheme.surfaceContainerHighest
	else JellyfinTheme.colorScheme.surfaceContainerHigh

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.graphicsLayer { scaleX = scale; scaleY = scale }
			.clip(RoundedCornerShape(12.dp))
			.background(bgColor)
			.onFocusChanged { focused = it.isFocused }
			.focusable()
			.clickable { onClick() }
			.padding(horizontal = 20.dp, vertical = 14.dp),
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.fillMaxWidth(),
		) {
			// Server icon
			Box(
				modifier = Modifier
					.size(40.dp)
					.clip(RoundedCornerShape(8.dp))
					.background(JellyfinTheme.colorScheme.primaryAccent.copy(alpha = 0.15f)),
				contentAlignment = Alignment.Center,
			) {
				Text(
					text = name.take(1).uppercase(),
					style = JellyfinTheme.typography.titleMedium,
					color = JellyfinTheme.colorScheme.primaryAccent,
				)
			}

			Spacer(modifier = Modifier.width(16.dp))

			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = name,
					style = JellyfinTheme.typography.bodyLarge,
					color = JellyfinTheme.colorScheme.onBackground,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
				Text(
					text = buildString {
						append(address)
						if (version != null) append(" • v$version")
					},
					style = JellyfinTheme.typography.labelSmall,
					color = JellyfinTheme.colorScheme.secondaryAccent,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
			}
		}
	}
}
