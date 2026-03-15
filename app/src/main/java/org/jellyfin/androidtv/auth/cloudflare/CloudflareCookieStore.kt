package org.jellyfin.androidtv.auth.cloudflare

import android.content.Context
import android.net.Uri
import androidx.core.content.edit

class CloudflareCookieStore(context: Context) {
	companion object {
		private const val PREFS_NAME = "cloudflare_cookies"
	}

	private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

	fun getCookie(serverUrl: String): String? {
		val host = Uri.parse(serverUrl).host ?: return null
		return prefs.getString(host, null)
	}

	fun setCookie(serverUrl: String, cookie: String) {
		val host = Uri.parse(serverUrl).host ?: return
		prefs.edit { putString(host, cookie) }
	}

	fun removeCookie(serverUrl: String) {
		val host = Uri.parse(serverUrl).host ?: return
		prefs.edit { remove(host) }
	}

	fun hasCookie(serverUrl: String): Boolean = getCookie(serverUrl) != null
}
