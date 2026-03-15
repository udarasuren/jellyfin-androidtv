package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.cloudflare.CloudflareDetector
import org.jellyfin.androidtv.auth.model.CloudflareAuthRequiredState
import org.jellyfin.androidtv.auth.model.ConnectingState
import org.jellyfin.androidtv.auth.model.ServerAdditionState
import org.jellyfin.androidtv.auth.model.UnableToConnectState
import org.jellyfin.androidtv.auth.repository.ServerRepository

class ServerAddViewModel(
	private val serverRepository: ServerRepository,
	private val cloudflareDetector: CloudflareDetector,
) : ViewModel() {
	private val _state = MutableStateFlow<ServerAdditionState?>(null)
	val state = _state.asStateFlow()

	private var lastAddress: String? = null

	fun addServer(address: String) {
		lastAddress = address
		serverRepository.addServer(address).onEach { state ->
			if (state is UnableToConnectState) {
				// Check if the failure is due to Cloudflare Zero Trust
				_state.value = ConnectingState(address)
				checkCloudflare(address, state)
			} else {
				_state.value = state
			}
		}.launchIn(viewModelScope)
	}

	private fun checkCloudflare(address: String, fallbackState: UnableToConnectState) {
		viewModelScope.launch {
			// Try common URL patterns for the address
			val candidates = buildList {
				if (address.startsWith("http")) add(address)
				else {
					add("https://$address")
					add("http://$address")
				}
			}

			val isCloudflare = candidates.any { cloudflareDetector.isCloudflareProtected(it) }

			_state.value = if (isCloudflare) {
				CloudflareAuthRequiredState(address)
			} else {
				fallbackState
			}
		}
	}

	fun retryLastServer() {
		lastAddress?.let { addServer(it) }
	}
}
