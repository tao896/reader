package com.htmake.reader.hotsearch

import java.io.IOException

data class HotSearchItem(
    val rank: Int,
    val title: String,
    val url: String,
    val hotValue: String = "",
    val summary: String = "",
    val imageUrl: String = "",
    val label: String = ""
)

data class HotSearchResult(
    val items: List<HotSearchItem>,
    val source: String,
    val sourceName: String
)

data class HotSearchPlatformConfig(
    val platformId: String,
    val platformName: String
)

interface HotSearchProvider {
    fun platformConfig(): HotSearchPlatformConfig
    suspend fun fetchHotSearches(): HotSearchResult
}

open class HotSearchAuthenticationException(message: String) : IOException(message)
