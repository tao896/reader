package com.htmake.reader.hotsearch

import com.google.gson.JsonParser
import java.io.IOException
import java.text.DecimalFormat

class BaiduHotSearchProvider : HotSearchProvider {

    companion object {
        private const val OFFICIAL_URL = "https://top.baidu.com/api/board?platform=pc&tab=realtime"
    }

    override fun platformConfig() = HotSearchPlatformConfig("baidu", "百度")

    override suspend fun fetchHotSearches(): HotSearchResult {
        val items = parseResponse(HotSearchHttp.get(OFFICIAL_URL, mapOf("Referer" to "https://top.baidu.com/board?tab=realtime")))
        if (items.isEmpty()) {
            throw IOException("百度官方接口未返回热搜数据")
        }
        return HotSearchResult(items, source = "official", sourceName = "百度官方")
    }

    fun parseResponse(json: String): List<HotSearchItem> {
        val root = JsonParser().parse(json).asJsonObject
        val cards = root.objectOrNull("data")?.arrayOrNull("cards") ?: return emptyList()
        val hotList = cards.firstOrNull { element ->
            element.isJsonObject && element.asJsonObject.string("component") == "hotList"
        }?.asJsonObject?.arrayOrNull("content") ?: return emptyList()

        return hotList.mapIndexedNotNull { index, element ->
            val item = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapIndexedNotNull null
            val title = item.string("word").ifEmpty { item.string("query") }
            if (title.isEmpty()) return@mapIndexedNotNull null
            HotSearchItem(
                rank = (item.intOrNull("index") ?: index) + 1,
                title = title,
                url = item.string("url").ifEmpty { item.string("rawUrl") },
                hotValue = formatHotScore(item.longOrNull("hotScore")),
                summary = item.string("desc"),
                imageUrl = item.string("img"),
                label = item.string("newHotName")
            )
        }
    }

    private fun formatHotScore(score: Long?): String {
        if (score == null || score <= 0L) return ""
        if (score < 10000L) return "$score 热度"
        return "${DecimalFormat("0.#").format(score / 10000.0)} 万热度"
    }
}
