package org.jellyfin.androidtv.ui.composable.billboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
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
private const val CROSSFADE_MS = 600

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

	// Auto-advance timer
	LaunchedEffect(currentIndex, isFocused) {
		if (!isFocused && items.size > 1) {
			delay(AUTO_ADVANCE_MS)
			currentIndex = (currentIndex + 1) % items.size
		}
	}

	Box(
		modifier = modifier
			.fillMaxWidth()
			.height(220.dp)
			.padding(horizontal = 48.dp, vertical = 8.dp)
			.onFocusChanged { state ->
				val hadFocus = isFocused
				isFocused = state.hasFocus
				if (state.hasFocus && !hadFocus) onFocusGained()
			}
			.onPreviewKeyEvent { event ->
				if (event.key == Key.DirectionUp && event.type == KeyEventType.KeyDown) {
					if (!canExitUp()) {
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
		// Compact banner: backdrop left, info right
		AnimatedContent(
			targetState = currentItem,
			transitionSpec = {
				fadeIn(tween(CROSSFADE_MS)) togetherWith fadeOut(tween(CROSSFADE_MS))
			},
			label = "BillboardTransition",
			modifier = Modifier.fillMaxSize(),
		) { item ->
			Row(
				modifier = Modifier
					.fillMaxSize()
					.clip(RoundedCornerShape(16.dp))
					.background(JellyfinTheme.colorScheme.surfaceContainerHigh),
			) {
				// Backdrop thumbnail
				CardImage(
					url = backdropUrlProvider(item),
					modifier = Modifier
						.fillMaxHeight()
						.aspectRatio(16f / 9f)
						.clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
				)

				// Info
				Column(
					modifier = Modifier
						.weight(1f)
						.fillMaxHeight()
						.padding(horizontal = 24.dp, vertical = 16.dp),
					verticalArrangement = Arrangement.Center,
				) {
					Text(
						text = item.name.orEmpty(),
						style = JellyfinTheme.typography.titleLarge,
						color = JellyfinTheme.colorScheme.onBackground,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
					)

					Spacer(modifier = Modifier.height(4.dp))

					val metaItems = buildList {
						item.productionYear?.let { add(it.toString()) }
						item.officialRating?.let { add(it) }
						item.genres?.take(2)?.let { addAll(it) }
					}
					if (metaItems.isNotEmpty()) {
						Text(
							text = metaItems.joinToString(" • "),
							style = JellyfinTheme.typography.bodyMedium,
							color = JellyfinTheme.colorScheme.secondaryAccent,
							maxLines = 1,
						)
					}

					item.overview?.let { overview ->
						Spacer(modifier = Modifier.height(6.dp))
						Text(
							text = overview,
							style = JellyfinTheme.typography.bodyMedium,
							color = JellyfinTheme.colorScheme.onBackground.copy(alpha = 0.6f),
							maxLines = 2,
							overflow = TextOverflow.Ellipsis,
						)
					}

					Spacer(modifier = Modifier.height(12.dp))

					Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
						Button(onClick = { onPlayClick(item) }) {
							Text(text = "\u25B6  Play", style = JellyfinTheme.typography.labelLarge)
						}
						Button(onClick = { onInfoClick(item) }) {
							Text(text = "More Info", style = JellyfinTheme.typography.labelLarge)
						}
					}
				}
			}
		}

		// Page indicator dots
		if (items.size > 1) {
			Row(
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.padding(bottom = 6.dp),
				horizontalArrangement = Arrangement.spacedBy(4.dp),
			) {
				items.forEachIndexed { index, _ ->
					val isActive = index == currentIndex % items.size
					Box(
						modifier = Modifier
							.size(if (isActive) 6.dp else 4.dp)
							.clip(CircleShape)
							.background(
								if (isActive) JellyfinTheme.colorScheme.primaryAccent
								else JellyfinTheme.colorScheme.secondaryAccent.copy(alpha = 0.3f)
							),
					)
				}
			}
		}
	}
}
