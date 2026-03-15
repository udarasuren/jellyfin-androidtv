package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.cloudflare.CloudflareAuthActivity
import org.jellyfin.androidtv.auth.model.CloudflareAuthRequiredState
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.ConnectingState
import org.jellyfin.androidtv.auth.model.UnableToConnectState
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.startup.ServerAddViewModel
import org.jellyfin.androidtv.util.getSummary
import org.koin.androidx.viewmodel.ext.android.viewModel

class ServerAddFragment : Fragment() {
	companion object {
		const val ARG_SERVER_ADDRESS = "server_address"
	}

	private val serverAddViewModel: ServerAddViewModel by viewModel()
	private val serverAddressArgument get() = arguments?.getString(ARG_SERVER_ADDRESS)?.ifBlank { null }

	private val cloudflareAuthLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == android.app.Activity.RESULT_OK) {
			serverAddViewModel.retryLastServer()
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		JellyfinTheme {
			AddServerScreen(
				viewModel = serverAddViewModel,
				initialAddress = serverAddressArgument,
				onCloudflareAuth = { address ->
					val url = if (address.startsWith("http")) address else "https://$address"
					cloudflareAuthLauncher.launch(CloudflareAuthActivity.createIntent(requireContext(), url))
				},
			)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		serverAddViewModel.state.onEach { state ->
			when (state) {
				is ConnectedState -> parentFragmentManager.commit {
					replace<StartupToolbarFragment>(R.id.content_view)
					add<ServerFragment>(
						R.id.content_view, null,
						bundleOf(ServerFragment.ARG_SERVER_ID to state.id.toString()),
					)
				}
				is CloudflareAuthRequiredState -> {
					val url = if (state.address.startsWith("http")) state.address else "https://${state.address}"
					cloudflareAuthLauncher.launch(CloudflareAuthActivity.createIntent(requireContext(), url))
				}
				else -> Unit
			}
		}.launchIn(lifecycleScope)
	}
}

@Composable
private fun AddServerScreen(
	viewModel: ServerAddViewModel,
	initialAddress: String?,
	onCloudflareAuth: (String) -> Unit,
) {
	val state by viewModel.state.collectAsState()
	var address by remember { mutableStateOf(initialAddress.orEmpty()) }
	val isAddressLocked = initialAddress != null

	val statusMessage = when (val s = state) {
		is ConnectingState -> "Connecting to ${s.address}..."
		is UnableToConnectState -> "Unable to connect. Check the address and try again."
		is CloudflareAuthRequiredState -> "Cloudflare authentication required..."
		else -> null
	}
	val isConnecting = state is ConnectingState
	val isError = state is UnableToConnectState

	// Auto-submit if address was pre-filled
	if (initialAddress != null) {
		androidx.compose.runtime.LaunchedEffect(Unit) {
			viewModel.addServer(initialAddress)
		}
	}

	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier.fillMaxWidth(0.4f),
		) {
			Text(
				text = "Add Server",
				style = JellyfinTheme.typography.displayMedium,
				color = JellyfinTheme.colorScheme.onBackground,
			)

			Spacer(modifier = Modifier.height(8.dp))

			Text(
				text = "Enter the address of your Jellyfin server",
				style = JellyfinTheme.typography.bodyMedium,
				color = JellyfinTheme.colorScheme.secondaryAccent,
			)

			Spacer(modifier = Modifier.height(32.dp))

			// Server address field
			Text(
				text = "Server Address",
				style = JellyfinTheme.typography.labelLarge,
				color = JellyfinTheme.colorScheme.secondaryAccent,
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 6.dp),
			)

			val bgColor = JellyfinTheme.colorScheme.surfaceContainerHigh
			val shape = JellyfinTheme.shapes.small

			BasicTextField(
				value = address,
				onValueChange = { address = it },
				enabled = !isAddressLocked && !isConnecting,
				singleLine = true,
				textStyle = JellyfinTheme.typography.bodyLarge.copy(
					color = JellyfinTheme.colorScheme.onBackground,
				),
				keyboardOptions = KeyboardOptions(
					keyboardType = KeyboardType.Uri,
					imeAction = ImeAction.Done,
				),
				keyboardActions = KeyboardActions(onDone = {
					if (address.isNotBlank()) viewModel.addServer(address)
				}),
				cursorBrush = SolidColor(JellyfinTheme.colorScheme.primaryAccent),
				decorationBox = { innerTextField ->
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.background(bgColor, shape)
							.padding(horizontal = 16.dp, vertical = 12.dp),
					) {
						if (address.isEmpty()) {
							Text(
								text = "https://your-server.com",
								style = JellyfinTheme.typography.bodyLarge,
								color = JellyfinTheme.colorScheme.secondaryAccent.copy(alpha = 0.4f),
							)
						}
						innerTextField()
					}
				},
				modifier = Modifier.fillMaxWidth(),
			)

			// Status message
			if (statusMessage != null) {
				Spacer(modifier = Modifier.height(12.dp))
				Text(
					text = statusMessage,
					style = JellyfinTheme.typography.bodyMedium,
					color = if (isError) JellyfinTheme.colorScheme.recording
					else JellyfinTheme.colorScheme.secondaryAccent,
					textAlign = TextAlign.Center,
				)
			}

			Spacer(modifier = Modifier.height(24.dp))

			Button(
				onClick = {
					if (address.isNotBlank()) viewModel.addServer(address)
				},
				enabled = !isConnecting,
			) {
				Text(text = "Connect", style = JellyfinTheme.typography.labelLarge)
			}
		}
	}
}
