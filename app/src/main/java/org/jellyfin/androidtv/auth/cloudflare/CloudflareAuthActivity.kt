package org.jellyfin.androidtv.auth.cloudflare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.FragmentActivity
import org.jellyfin.androidtv.R
import timber.log.Timber

/**
 * Activity that shows a WebView for Cloudflare Zero Trust authentication.
 * Loads the server URL, lets the user authenticate via Cloudflare Access,
 * and extracts the CF_Authorization cookie when authentication completes.
 */
class CloudflareAuthActivity : FragmentActivity() {
	companion object {
		const val EXTRA_SERVER_URL = "server_url"
		const val RESULT_AUTHENTICATED = "cf_authenticated"

		fun createIntent(context: Context, serverUrl: String): Intent =
			Intent(context, CloudflareAuthActivity::class.java).apply {
				putExtra(EXTRA_SERVER_URL, serverUrl)
			}
	}

	private lateinit var webView: WebView
	private lateinit var serverUrl: String
	private lateinit var cookieStore: CloudflareCookieStore

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		serverUrl = intent.getStringExtra(EXTRA_SERVER_URL)
			?: run { finish(); return }

		cookieStore = CloudflareCookieStore(this)

		webView = WebView(this).apply {
			settings.javaScriptEnabled = true
			settings.domStorageEnabled = true
			settings.databaseEnabled = true
		}
		setContentView(webView)
		setTitle(R.string.cloudflare_auth_title)

		// Enable cookies in WebView
		val cookieManager = CookieManager.getInstance()
		cookieManager.setAcceptCookie(true)
		cookieManager.setAcceptThirdPartyCookies(webView, true)

		// Clear existing cookies for this domain to force fresh auth
		cookieManager.removeAllCookies(null)

		webView.webViewClient = object : WebViewClient() {
			override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
				// Stay within the WebView for all navigation
				return false
			}

			override fun onPageFinished(view: WebView?, url: String?) {
				super.onPageFinished(view, url)
				checkForCfCookie(url)
			}
		}

		Timber.i("Loading Cloudflare auth for %s", serverUrl)
		webView.loadUrl(serverUrl)
	}

	private fun checkForCfCookie(url: String?) {
		if (url == null) return

		val cookieManager = CookieManager.getInstance()
		val cookies = cookieManager.getCookie(url) ?: return

		// Parse cookies to find CF_Authorization
		val cfCookie = cookies.split(";")
			.map { it.trim() }
			.firstOrNull { it.startsWith("CF_Authorization=") }
			?.substringAfter("CF_Authorization=")

		if (cfCookie != null) {
			Timber.i("CF_Authorization cookie obtained for %s", serverUrl)
			cookieStore.setCookie(serverUrl, cfCookie)
			setResult(RESULT_OK, Intent().apply {
				putExtra(RESULT_AUTHENTICATED, true)
			})
			finish()
		}
	}

	@Suppress("DEPRECATION")
	override fun onBackPressed() {
		if (webView.canGoBack()) {
			webView.goBack()
		} else {
			setResult(RESULT_CANCELED)
			super.onBackPressed()
		}
	}

	override fun onDestroy() {
		webView.destroy()
		super.onDestroy()
	}
}
