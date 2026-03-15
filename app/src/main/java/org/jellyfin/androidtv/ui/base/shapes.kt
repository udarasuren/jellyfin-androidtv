package org.jellyfin.androidtv.ui.base

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

object ShapeDefaults {
	val ExtraSmall: CornerBasedShape = RoundedCornerShape(4.0.dp)
	val Small: CornerBasedShape = RoundedCornerShape(8.0.dp)
	val Medium: CornerBasedShape = RoundedCornerShape(12.0.dp)
	val Large: CornerBasedShape = RoundedCornerShape(16.0.dp)
	val ExtraLarge: CornerBasedShape = RoundedCornerShape(28.0.dp)

	// Premium card shapes
	val Card: CornerBasedShape = RoundedCornerShape(8.0.dp)
	val CardFocused: CornerBasedShape = RoundedCornerShape(12.0.dp)
	val Billboard: CornerBasedShape = RoundedCornerShape(0.0.dp)
	val Overlay: CornerBasedShape = RoundedCornerShape(topStart = 16.0.dp, topEnd = 16.0.dp)
}

@Immutable
data class Shapes(
	val extraSmall: CornerBasedShape = ShapeDefaults.ExtraSmall,
	val small: CornerBasedShape = ShapeDefaults.Small,
	val medium: CornerBasedShape = ShapeDefaults.Medium,
	val large: CornerBasedShape = ShapeDefaults.Large,
	val extraLarge: CornerBasedShape = ShapeDefaults.ExtraLarge,
	val card: CornerBasedShape = ShapeDefaults.Card,
	val cardFocused: CornerBasedShape = ShapeDefaults.CardFocused,
	val billboard: CornerBasedShape = ShapeDefaults.Billboard,
	val overlay: CornerBasedShape = ShapeDefaults.Overlay,
)

val LocalShapes = staticCompositionLocalOf { Shapes() }
