package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.ApiClientErrorLoginState
import org.jellyfin.androidtv.auth.model.AuthenticatedState
import org.jellyfin.androidtv.auth.model.AuthenticatingState
import org.jellyfin.androidtv.auth.model.RequireSignInState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.ServerUnavailableState
import org.jellyfin.androidtv.auth.model.ServerVersionNotSupported
import org.jellyfin.androidtv.auth.model.User
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.ProfilePicture
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.startup.StartupViewModel
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ServerFragment : Fragment() {
	companion object {
		const val ARG_SERVER_ID = "server_id"
	}

	private val startupViewModel: StartupViewModel by activityViewModel()
	private val backgroundService: BackgroundService by inject()

	private val serverIdArgument get() = arguments?.getString(ARG_SERVER_ID)?.ifBlank { null }?.toUUIDOrNull()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val server = serverIdArgument?.let(startupViewModel::getServer)

		if (server == null) {
			navigateFragment<SelectServerFragment>(keepToolbar = true, keepHistory = false)
			return null
		}

		startupViewModel.loadUsers(server)

		lifecycleScope.launch {
			lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
				val updated = startupViewModel.updateServer(server)
				if (updated) startupViewModel.getServer(server.id)
			}
		}

		return content {
			JellyfinTheme {
				WhoIsWatchingScreen(
					server = server,
					viewModel = startupViewModel,
					onUserClick = { user -> handleUserClick(server, user) },
					onAddUser = {
						navigateFragment<UserLoginFragment>(
							args = bundleOf(
								UserLoginFragment.ARG_SERVER_ID to server.id.toString(),
								UserLoginFragment.ARG_USERNAME to null,
							)
						)
					},
					onSwitchServer = {
						navigateFragment<SelectServerFragment>(keepToolbar = true)
					},
				)
			}
		}
	}

	private fun handleUserClick(server: Server, user: User) {
		startupViewModel.authenticate(server, user).onEach { state ->
			when (state) {
				AuthenticatingState -> Unit
				AuthenticatedState -> Unit
				RequireSignInState -> navigateFragment<UserLoginFragment>(
					bundleOf(
						UserLoginFragment.ARG_SERVER_ID to server.id.toString(),
						UserLoginFragment.ARG_USERNAME to user.name,
					)
				)
				ServerUnavailableState,
				is ApiClientErrorLoginState -> Toast.makeText(context, R.string.server_connection_failed, Toast.LENGTH_LONG).show()
				is ServerVersionNotSupported -> Toast.makeText(
					context,
					getString(R.string.server_unsupported_notification, state.server.version, ServerRepository.recommendedServerVersion.toString()),
					Toast.LENGTH_LONG,
				).show()
			}
		}.launchIn(lifecycleScope)
	}

	private inline fun <reified F : Fragment> navigateFragment(
		args: Bundle = bundleOf(),
		keepToolbar: Boolean = false,
		keepHistory: Boolean = true,
	) {
		requireActivity().supportFragmentManager.commit {
			if (keepToolbar) {
				replace<StartupToolbarFragment>(R.id.content_view)
				add<F>(R.id.content_view, null, args)
			} else {
				replace<F>(R.id.content_view, null, args)
			}
			if (keepHistory) addToBackStack(null)
		}
	}

	override fun onResume() {
		super.onResume()
		startupViewModel.reloadStoredServers()
		backgroundService.clearBackgrounds()
		val server = serverIdArgument?.let(startupViewModel::getServer)
		if (server != null) startupViewModel.loadUsers(server)
		else navigateFragment<SelectServerFragment>(keepToolbar = true)
	}
}

@Composable
private fun WhoIsWatchingScreen(
	server: Server,
	viewModel: StartupViewModel,
	onUserClick: (User) -> Unit,
	onAddUser: () -> Unit,
	onSwitchServer: () -> Unit,
) {
	val users by viewModel.users.collectAsState(initial = emptyList())

	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier.fillMaxWidth(),
		) {
			// Title
			Text(
				text = "Who's watching?",
				style = JellyfinTheme.typography.displayMedium,
				color = JellyfinTheme.colorScheme.onBackground,
			)

			Spacer(modifier = Modifier.height(48.dp))

			// User profiles
			if (users.isNotEmpty()) {
				LazyRow(
					horizontalArrangement = Arrangement.spacedBy(24.dp),
					contentPadding = PaddingValues(horizontal = 48.dp),
				) {
					items(users, key = { it.id }) { user ->
						ProfileCard(
							name = user.name,
							imageUrl = viewModel.getUserImage(server, user),
							onClick = { onUserClick(user) },
						)
					}
				}
			} else {
				Text(
					text = "No accounts found.\nAdd an account to get started.",
					style = JellyfinTheme.typography.bodyLarge,
					color = JellyfinTheme.colorScheme.secondaryAccent,
					textAlign = TextAlign.Center,
				)
			}

			Spacer(modifier = Modifier.height(48.dp))

			// Action buttons
			LazyRow(
				horizontalArrangement = Arrangement.spacedBy(16.dp),
			) {
				item {
					Button(onClick = onAddUser) {
						Text(
							text = "Add Account",
							style = JellyfinTheme.typography.labelLarge,
						)
					}
				}
				item {
					Button(onClick = onSwitchServer) {
						Text(
							text = "Switch Server",
							style = JellyfinTheme.typography.labelLarge,
						)
					}
				}
			}

			// Server info
			Spacer(modifier = Modifier.height(24.dp))
			Text(
				text = "${server.name} • ${server.address}",
				style = JellyfinTheme.typography.labelSmall,
				color = JellyfinTheme.colorScheme.secondaryAccent.copy(alpha = 0.5f),
			)
		}
	}
}

@Composable
private fun ProfileCard(
	name: String,
	imageUrl: String?,
	onClick: () -> Unit,
) {
	var focused by remember { mutableStateOf(false) }
	val scale by animateFloatAsState(
		targetValue = if (focused) 1.1f else 1f,
		animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
		label = "profileScale",
	)
	val borderColor = if (focused) JellyfinTheme.colorScheme.primaryAccent else Color.Transparent

	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.width(120.dp)
			.graphicsLayer {
				scaleX = scale
				scaleY = scale
			}
			.onFocusChanged { focused = it.isFocused }
			.focusable()
			.clickable { onClick() },
	) {
		// Profile picture
		Box(
			modifier = Modifier
				.size(100.dp)
				.clip(CircleShape)
				.border(3.dp, borderColor, CircleShape)
				.background(JellyfinTheme.colorScheme.surfaceContainerHigh, CircleShape),
		) {
			ProfilePicture(
				url = imageUrl,
				iconPadding = PaddingValues(20.dp),
				modifier = Modifier.fillMaxSize(),
			)
		}

		Spacer(modifier = Modifier.height(12.dp))

		// Name
		Text(
			text = name,
			style = JellyfinTheme.typography.bodyMedium,
			color = if (focused) JellyfinTheme.colorScheme.onBackground else JellyfinTheme.colorScheme.secondaryAccent,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			textAlign = TextAlign.Center,
		)
	}
}
