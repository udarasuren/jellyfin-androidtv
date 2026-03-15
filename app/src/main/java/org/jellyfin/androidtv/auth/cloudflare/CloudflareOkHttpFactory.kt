package org.jellyfin.androidtv.auth.cloudflare

import okhttp3.OkHttpClient
import org.jellyfin.sdk.api.okhttp.OkHttpFactory

/**
 * Creates an [OkHttpFactory] with the Cloudflare authentication interceptor
 * injected into the base OkHttpClient. All HTTP clients created by the SDK
 * will inherit this interceptor.
 */
fun createCloudflareOkHttpFactory(interceptor: CloudflareAuthInterceptor): OkHttpFactory {
	val baseClient = OkHttpClient.Builder()
		.addInterceptor(interceptor)
		.build()
	return OkHttpFactory(baseClient)
}
