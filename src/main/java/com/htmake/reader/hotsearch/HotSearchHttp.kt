package com.htmake.reader.hotsearch

import io.legado.app.help.http.okHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

internal object HotSearchHttp {
    private const val USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String {
        return withContext(Dispatchers.IO) {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json,text/plain,*/*")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
            headers.forEach { (name, value) -> requestBuilder.header(name, value) }

            val client = okHttpClient.newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code} (${response.request.url})")
                }
                response.body?.string() ?: throw IOException("Empty response (${response.request.url})")
            }
        }
    }
}
