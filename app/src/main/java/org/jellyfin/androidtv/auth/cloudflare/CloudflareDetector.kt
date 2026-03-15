package org.jellyfin.androidtv.auth.cloudflare

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Detects if a server URL is behind Cloudflare Zero Trust by making
 * a HEAD request and checking for Cloudflare challenge indicators.
 */
class CloudflareDetector {
	companion object {
		private val CF_INDICATORS = listOf(
			"cf-mitigated",
			"cf-chl-bypass",
		)
	}

	private val client = OkHttpClient.Builder()
		.connectTimeout(10, TimeUnit.SECONDS)
		.readTimeout(10, TimeUnit.SECONDS)
		.followRedirects(false)
		.build()

	suspend fun isCloudflareProtected(serverUrl: String): Boolean = withContext(Dispatchers.IO) {
		try {
			val request = Request.Builder()
				.url(serverUrl)
				.head()
				.build()

			client.newCall(request).execute().use { response ->
				val isCloudflare = response.code == 403 && (
					response.headers.names().any { it.lowercase() in CF_INDICATORS } ||
						response.header("server")?.lowercase()?.contains("cloudflare") == true
					)

				if (isCloudflare) {
					Timber.i("Cloudflare Zero Trust detected for %s", serverUrl)
				}

				isCloudflare
			}
		} catch (e: Exception) {
			Timber.w(e, "Failed to check Cloudflare status for %s", serverUrl)
			false
		}
	}
}
