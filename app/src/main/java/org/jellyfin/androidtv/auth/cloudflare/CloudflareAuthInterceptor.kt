package org.jellyfin.androidtv.auth.cloudflare

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * OkHttp interceptor that attaches CF_Authorization cookies to requests
 * and detects expired Cloudflare tokens (403 responses with CF headers).
 *
 * When a cookie expires during an active session, it emits the server URL
 * on [authExpired] so the UI can trigger re-authentication.
 */
class CloudflareAuthInterceptor(
	private val cookieStore: CloudflareCookieStore,
) : Interceptor {
	private val _authExpired = MutableSharedFlow<String>(extraBufferCapacity = 1)

	/** Emits the server base URL when a CF_Authorization cookie has expired. */
	val authExpired: SharedFlow<String> = _authExpired.asSharedFlow()

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val url = request.url.toString()
		val cookie = cookieStore.getCookie(url)

		val newRequest = if (cookie != null) {
			Timber.d("Attaching CF_Authorization cookie for %s", request.url.host)
			val existingCookies = request.header("Cookie")
			val cookieHeader = if (existingCookies != null) {
				"$existingCookies; CF_Authorization=$cookie"
			} else {
				"CF_Authorization=$cookie"
			}
			request.newBuilder()
				.header("Cookie", cookieHeader)
				.build()
		} else {
			request
		}

		val response = chain.proceed(newRequest)

		// Detect expired CF cookie: we had a cookie but got a 403 with Cloudflare headers
		if (cookie != null && response.code == 403 && isCloudflareResponse(response)) {
			Timber.w("CF_Authorization cookie expired for %s", request.url.host)
			cookieStore.removeCookie(url)
			val baseUrl = "${request.url.scheme}://${request.url.host}" +
				if (request.url.port != 443 && request.url.port != 80) ":${request.url.port}" else ""
			_authExpired.tryEmit(baseUrl)
		}

		return response
	}

	private fun isCloudflareResponse(response: Response): Boolean {
		val headers = response.headers.names().map { it.lowercase() }
		return headers.any { it.startsWith("cf-") } ||
			response.header("server")?.lowercase()?.contains("cloudflare") == true
	}
}
