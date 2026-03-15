package org.jellyfin.androidtv.ui.composable.billboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.composable.card.CardImage
import org.jellyfin.sdk.model.api.BaseItemDto

private const val AUTO_ADVANCE_MS = 8000L
private const val CROSSFADE_MS = 800
private const val ZOOM_DURATION_MS = 12000

@Composable
fun HeroBillboard(
	items: List<BaseItemDto>,
	backdropUrlProvider: (BaseItemDto) -> String?,
	modifier: Modifier = Modifier,
	onPlayClick: (BaseItemDto) -> Unit = {},
	onInfoClick: (BaseItemDto) -> Unit = {},
	onFocusGained: () -> Unit = {},
	canExitUp: () -> Boolean = { true },
) {
	if (items.isEmpty()) return

	var currentIndex by remember { mutableIntStateOf(0) }
	var isFocused by remember { mutableStateOf(false) }
	val currentItem = items[currentIndex % items.size]

	// Slow zoom animation on the backdrop (Ken Burns effect)
	var zoomTarget by remember { mutableStateOf(1.02f) }
	LaunchedEffect(currentIndex) {
		zoomTarget = 1.0f
		// Small delay then start zooming
		delay(100)
		zoomTarget = 1.06f
	}
	val zoomScale by animateFloatAsState(
		targetValue = zoomTarget,
		animationSpec = tween(durationMillis = ZOOM_DURATION_MS),
		label = "billboardZoom",
	)

	// Auto-advance timer
	LaunchedEffect(currentIndex, isFocused) {
		if (!isFocused && items.size > 1) {
			delay(AUTO_ADVANCE_MS)
			currentIndex = (currentIndex + 1) % items.size
		}
	}

	// Text entrance key — triggers staggered animation on item change
	var textVisible by remember { mutableStateOf(false) }
	LaunchedEffect(currentIndex) {
		textVisible = false
		delay(300) // Wait for crossfade to start
		textVisible = true
	}

	Box(
		modifier = modifier
			.fillMaxWidth()
			.height(400.dp)
			.onFocusChanged { state ->
				val hadFocus = isFocused
				isFocused = state.hasFocus
				if (state.hasFocus && !hadFocus) onFocusGained()
			}
			.onPreviewKeyEvent { event ->
				// Intercept UP key before children — block until billboard is fully scrolled into view
				if (event.key == Key.DirectionUp && event.type == KeyEventType.KeyDown) {
					if (!canExitUp()) {
						// Trigger scroll to top, consume the event
						onFocusGained()
						true
					} else false
				} else false
			}
			.onKeyEvent { event ->
				if (event.type == KeyEventType.KeyUp) {
					when (event.key) {
						Key.DirectionLeft -> {
							currentIndex = if (currentIndex > 0) currentIndex - 1 else items.size - 1
							true
						}
						Key.DirectionRight -> {
							currentIndex = (currentIndex + 1) % items.size
							true
						}
						else -> false
					}
				} else false
			},
	) {
		// Backdrop with crossfade + zoom
		AnimatedContent(
			targetState = currentItem,
			transitionSpec = {
				fadeIn(tween(CROSSFADE_MS)) togetherWith fadeOut(tween(CROSSFADE_MS))
			},
			label = "BillboardTransition",
		) { item ->
			CardImage(
				url = backdropUrlProvider(item),
				modifier = Modifier
					.fillMaxSize()
					.graphicsLayer {
						scaleX = zoomScale
						scaleY = zoomScale
					},
			)
		}

		// Bottom gradient scrim
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(280.dp)
				.align(Alignment.BottomCenter)
				.background(
					Brush.verticalGradient(
						colors = listOf(
							Color.Transparent,
							JellyfinTheme.colorScheme.background.copy(alpha = 0.5f),
							JellyfinTheme.colorScheme.background.copy(alpha = 0.85f),
							JellyfinTheme.colorScheme.background,
						),
					)
				),
		)

		// Content overlay with staggered entrance
		Column(
			modifier = Modifier
				.align(Alignment.BottomStart)
				.padding(start = 48.dp, end = 48.dp, bottom = 32.dp),
		) {
			// Title — slides in first
			AnimatedVisibility(
				visible = textVisible,
				enter = fadeIn(tween(400)) + slideInVertically(tween(500)) { it / 2 },
			) {
				Text(
					text = currentItem.name.orEmpty(),
					style = JellyfinTheme.typography.displayMedium,
					color = Color.White,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
			}

			Spacer(modifier = Modifier.height(4.dp))

			// Metadata — slides in with delay
			val metaItems = buildList {
				currentItem.productionYear?.let { add(it.toString()) }
				currentItem.officialRating?.let { add(it) }
				currentItem.genres?.take(3)?.let { addAll(it) }
			}
			if (metaItems.isNotEmpty()) {
				AnimatedVisibility(
					visible = textVisible,
					enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(tween(500, delayMillis = 100)) { it / 2 },
				) {
					Text(
						text = metaItems.joinToString(" \u2022 "),
						style = JellyfinTheme.typography.bodyMedium,
						color = Color.White.copy(alpha = 0.8f),
						maxLines = 1,
					)
				}
			}

			Spacer(modifier = Modifier.height(8.dp))

			// Synopsis — slides in with more delay
			currentItem.overview?.let { overview ->
				AnimatedVisibility(
					visible = textVisible,
					enter = fadeIn(tween(400, delayMillis = 200)) + slideInVertically(tween(500, delayMillis = 200)) { it / 2 },
				) {
					Text(
						text = overview,
						style = JellyfinTheme.typography.bodyMedium,
						color = Color.White.copy(alpha = 0.7f),
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.fillMaxWidth(0.5f),
					)
				}
			}

			Spacer(modifier = Modifier.height(16.dp))

			// Buttons — slide in last
			AnimatedVisibility(
				visible = textVisible,
				enter = fadeIn(tween(400, delayMillis = 300)) + slideInVertically(tween(500, delayMillis = 300)) { it / 2 },
			) {
				Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					Button(onClick = { onPlayClick(currentItem) }) {
						Text(text = "\u25B6  Play", style = JellyfinTheme.typography.labelLarge)
					}
					Button(onClick = { onInfoClick(currentItem) }) {
						Text(text = "More Info", style = JellyfinTheme.typography.labelLarge)
					}
				}
			}
		}

		// Page indicator dots
		if (items.size > 1) {
			Row(
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.padding(bottom = 12.dp),
				horizontalArrangement = Arrangement.spacedBy(6.dp),
			) {
				items.forEachIndexed { index, _ ->
					val isActive = index == currentIndex % items.size
					Box(
						modifier = Modifier
							.size(if (isActive) 8.dp else 6.dp)
							.clip(CircleShape)
							.background(
								if (isActive) Color.White
								else Color.White.copy(alpha = 0.4f)
							),
					)
				}
			}
		}
	}
}
