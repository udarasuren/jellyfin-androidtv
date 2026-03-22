-dontwarn org.commonmark.ext.gfm.strikethrough.Strikethrough

# Keep all ViewModels (Koin creates them via reflection)
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Cloudflare auth classes
-keep class org.jellyfin.androidtv.auth.cloudflare.** { *; }

# Keep Compose-related classes from being stripped
-keep class org.jellyfin.androidtv.ui.composable.** { *; }
-keep class org.jellyfin.androidtv.ui.home.HomeViewModel { *; }
-keep class org.jellyfin.androidtv.ui.home.HomeScreenState { *; }
-keep class org.jellyfin.androidtv.ui.home.HomeRow { *; }
-keep class org.jellyfin.androidtv.ui.home.HomeRowType { *; }
-keep class org.jellyfin.androidtv.ui.composable.detail.DetailOverlayViewModel { *; }
-keep class org.jellyfin.androidtv.ui.composable.detail.DetailState { *; }
