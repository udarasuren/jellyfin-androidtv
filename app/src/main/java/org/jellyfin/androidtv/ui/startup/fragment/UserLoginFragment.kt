package org.jellyfin.androidtv.ui.startup.fragment

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.Lifecycle
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
import org.jellyfin.androidtv.auth.model.ServerUnavailableState
import org.jellyfin.androidtv.auth.model.ServerVersionNotSupported
import org.jellyfin.androidtv.auth.model.UnavailableQuickConnectState
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.startup.UserLoginViewModel
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class UserLoginFragment : Fragment() {
	companion object {
		const val ARG_USERNAME = "user_name"
		const val ARG_SERVER_ID = "server_id"
		const val ARG_SKIP_QUICKCONNECT = "skip_quickconnect"
		const val TAG_LOGIN_METHOD = "login_method"
	}

	private val userLoginViewModel: UserLoginViewModel by activityViewModel()
	private val backgroundService: BackgroundService by inject()

	private val usernameArgument get() = arguments?.getString(ARG_USERNAME)?.ifBlank { null }
	private val serverIdArgument get() = arguments?.getString(ARG_SERVER_ID)?.ifBlank { null }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		userLoginViewModel.forcedUsername = usernameArgument
		userLoginViewModel.setServer(serverIdArgument?.toUUIDOrNull())
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		JellyfinTheme {
			LoginScreen(
				viewModel = userLoginViewModel,
				forcedUsername = usernameArgument,
				onCancel = { parentFragmentManager.popBackStack() },
			)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		userLoginViewModel.clearLoginState()

		lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				userLoginViewModel.server.onEach { server ->
					if (server != null) backgroundService.setBackground(server)
					else backgroundService.clearBackgrounds()
				}.launchIn(this)
			}
		}
	}
}

@Composable
private fun LoginScreen(
	viewModel: UserLoginViewModel,
	forcedUsername: String?,
	onCancel: () -> Unit,
) {
	val server by viewModel.server.collectAsState()
	val loginState by viewModel.loginState.collectAsState()
	val quickConnectState by viewModel.quickConnectState.collectAsState()

	var username by remember { mutableStateOf(forcedUsername.orEmpty()) }
	var password by remember { mutableStateOf("") }
	val passwordFocusRequester = remember { FocusRequester() }
	val isUsernameEditable = forcedUsername == null

	val statusMessage = when (loginState) {
		AuthenticatingState -> "Signing in..."
		RequireSignInState -> "Invalid username or password"
		ServerUnavailableState, is ApiClientErrorLoginState -> "Unable to connect to server"
		is ServerVersionNotSupported -> "Server version not supported"
		AuthenticatedState, null -> null
	}

	// Auto-focus password if username is pre-filled
	LaunchedEffect(isUsernameEditable) {
		if (!isUsernameEditable) passwordFocusRequester.requestFocus()
	}

	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier.fillMaxWidth(0.4f),
		) {
			// Title
			Text(
				text = "Sign In",
				style = JellyfinTheme.typography.displayMedium,
				color = JellyfinTheme.colorScheme.onBackground,
			)

			Spacer(modifier = Modifier.height(8.dp))

			Text(
				text = server?.name ?: "Jellyfin",
				style = JellyfinTheme.typography.bodyMedium,
				color = JellyfinTheme.colorScheme.secondaryAccent,
			)

			Spacer(modifier = Modifier.height(32.dp))

			// Username field
			Text(
				text = "Username",
				style = JellyfinTheme.typography.labelLarge,
				color = JellyfinTheme.colorScheme.secondaryAccent,
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 6.dp),
			)
			StyledTextField(
				value = username,
				onValueChange = { username = it },
				enabled = isUsernameEditable,
				keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
				keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
			)

			Spacer(modifier = Modifier.height(16.dp))

			// Password field
			Text(
				text = "Password",
				style = JellyfinTheme.typography.labelLarge,
				color = JellyfinTheme.colorScheme.secondaryAccent,
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 6.dp),
			)
			StyledTextField(
				value = password,
				onValueChange = { password = it },
				isPassword = true,
				modifier = Modifier.focusRequester(passwordFocusRequester),
				keyboardOptions = KeyboardOptions(
					keyboardType = KeyboardType.Password,
					imeAction = ImeAction.Done,
				),
				keyboardActions = KeyboardActions(onDone = {
					if (username.isNotBlank()) viewModel.login(username, password)
				}),
			)

			// Status message
			if (statusMessage != null) {
				Spacer(modifier = Modifier.height(12.dp))
				Text(
					text = statusMessage,
					style = JellyfinTheme.typography.bodyMedium,
					color = if (loginState == AuthenticatingState) JellyfinTheme.colorScheme.secondaryAccent
					else JellyfinTheme.colorScheme.recording,
					textAlign = TextAlign.Center,
				)
			}

			Spacer(modifier = Modifier.height(24.dp))

			// Buttons
			Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
				Button(
					onClick = {
						if (username.isNotBlank()) viewModel.login(username, password)
					},
				) {
					Text(text = "Sign In", style = JellyfinTheme.typography.labelLarge)
				}

				Button(onClick = onCancel) {
					Text(text = "Cancel", style = JellyfinTheme.typography.labelLarge)
				}
			}
		}
	}
}

@Composable
private fun StyledTextField(
	value: String,
	onValueChange: (String) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	isPassword: Boolean = false,
	keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
	keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
	val bgColor = JellyfinTheme.colorScheme.surfaceContainerHigh
	val shape = JellyfinTheme.shapes.small

	BasicTextField(
		value = value,
		onValueChange = onValueChange,
		enabled = enabled,
		singleLine = true,
		textStyle = JellyfinTheme.typography.bodyLarge.copy(
			color = JellyfinTheme.colorScheme.onBackground,
		),
		visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
		keyboardOptions = keyboardOptions,
		keyboardActions = keyboardActions,
		cursorBrush = SolidColor(JellyfinTheme.colorScheme.primaryAccent),
		decorationBox = { innerTextField ->
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.background(bgColor, shape)
					.padding(horizontal = 16.dp, vertical = 12.dp),
			) {
				if (value.isEmpty()) {
					Text(
						text = if (isPassword) "Enter password" else "Enter username",
						style = JellyfinTheme.typography.bodyLarge,
						color = JellyfinTheme.colorScheme.secondaryAccent.copy(alpha = 0.4f),
					)
				}
				innerTextField()
			}
		},
		modifier = modifier.fillMaxWidth(),
	)
}
