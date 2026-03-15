package org.jellyfin.androidtv.ui.base

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

object TypographyDefaults {
	val Default: TextStyle = TextStyle.Default

	// Display styles — hero billboards and large featured content
	val DisplayLarge: TextStyle = Default.copy(
		fontSize = 48.sp,
		lineHeight = 52.sp,
		fontWeight = FontWeight.W700,
		letterSpacing = (-0.5).sp,
	)
	val DisplayMedium: TextStyle = Default.copy(
		fontSize = 32.sp,
		lineHeight = 38.sp,
		fontWeight = FontWeight.W700,
		letterSpacing = (-0.25).sp,
	)

	// Title styles — section headers and card titles
	val TitleLarge: TextStyle = Default.copy(
		fontSize = 24.sp,
		lineHeight = 30.sp,
		fontWeight = FontWeight.W600,
	)
	val TitleMedium: TextStyle = Default.copy(
		fontSize = 20.sp,
		lineHeight = 26.sp,
		fontWeight = FontWeight.W600,
	)

	// Body styles — descriptions and synopsis
	val BodyLarge: TextStyle = Default.copy(
		fontSize = 16.sp,
		lineHeight = 24.sp,
		fontWeight = FontWeight.W400,
	)
	val BodyMedium: TextStyle = Default.copy(
		fontSize = 14.sp,
		lineHeight = 20.sp,
		fontWeight = FontWeight.W400,
	)

	// Label styles — metadata, tags, and small text
	val LabelLarge: TextStyle = Default.copy(
		fontSize = 14.sp,
		lineHeight = 18.sp,
		fontWeight = FontWeight.W500,
		letterSpacing = 0.1.sp,
	)
	val LabelSmall: TextStyle = Default.copy(
		fontSize = 11.sp,
		lineHeight = 14.sp,
		fontWeight = FontWeight.W500,
		letterSpacing = 0.5.sp,
	)

	// Existing list styles (kept for backwards compatibility)
	val ListHeader: TextStyle = Default.copy(
		fontSize = 15.sp,
		lineHeight = 20.sp,
		fontWeight = FontWeight.W700,
	)
	val ListOverline: TextStyle = Default.copy(
		fontSize = 10.sp,
		lineHeight = 12.sp,
		fontWeight = FontWeight.W600,
		letterSpacing = 0.65.sp,
	)
	val ListHeadline: TextStyle = Default.copy(
		fontSize = 14.sp,
		lineHeight = 28.sp,
		fontWeight = FontWeight.W600,
	)
	val ListCaption: TextStyle = Default.copy(
		fontSize = 11.sp,
		lineHeight = 14.sp,
		fontWeight = FontWeight.W500,
		letterSpacing = 0.1.sp,
	)

	val Badge: TextStyle = Default.copy(
		fontSize = 11.sp,
		fontWeight = FontWeight.W700,
		textAlign = TextAlign.Center,
	)
}

@Immutable
data class Typography(
	val default: TextStyle = TypographyDefaults.Default,
	val displayLarge: TextStyle = TypographyDefaults.DisplayLarge,
	val displayMedium: TextStyle = TypographyDefaults.DisplayMedium,
	val titleLarge: TextStyle = TypographyDefaults.TitleLarge,
	val titleMedium: TextStyle = TypographyDefaults.TitleMedium,
	val bodyLarge: TextStyle = TypographyDefaults.BodyLarge,
	val bodyMedium: TextStyle = TypographyDefaults.BodyMedium,
	val labelLarge: TextStyle = TypographyDefaults.LabelLarge,
	val labelSmall: TextStyle = TypographyDefaults.LabelSmall,
	val listHeader: TextStyle = TypographyDefaults.ListHeader,
	val listOverline: TextStyle = TypographyDefaults.ListOverline,
	val listHeadline: TextStyle = TypographyDefaults.ListHeadline,
	val listCaption: TextStyle = TypographyDefaults.ListCaption,
	val badge: TextStyle = TypographyDefaults.Badge,
)

val LocalTypography = staticCompositionLocalOf { Typography() }
