package com.htmake.reader.hotsearch

import com.google.gson.JsonParser
import java.io.IOException

class ZhihuHotSearchProvider : HotSearchProvider {

    companion object {
        private const val OFFICIAL_URL = "https://api.zhihu.com/topstory/hot-lists/total?limit=50"
    }

    override fun platformConfig() = HotSearchPlatformConfig("zhihu", "知乎")

    override suspend fun fetchHotSearches(): HotSearchResult {
        val items = parseResponse(HotSearchHttp.get(OFFICIAL_URL, mapOf("Referer" to "https://www.zhihu.com/hot")))
        if (items.isEmpty()) {
            throw IOException("知乎官方接口未返回热搜数据")
        }
        return HotSearchResult(items, source = "official", sourceName = "知乎官方")
    }

    fun parseResponse(json: String): List<HotSearchItem> {
        val root = JsonParser().parse(json).asJsonObject
        val data = root.arrayOrNull("data") ?: return emptyList()
        return data.mapIndexedNotNull { index, element ->
            val card = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapIndexedNotNull null
            val target = card.objectOrNull("target") ?: return@mapIndexedNotNull null
            val title = target.string("title")
            if (title.isEmpty()) return@mapIndexedNotNull null
            val targetId = target.string("id")
            val targetType = target.string("type")
            val imageUrl = card.arrayOrNull("children")
                ?.firstOrNull()
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.string("thumbnail")
                .orEmpty()
            HotSearchItem(
                rank = index + 1,
                title = title,
                url = publicUrl(targetType, targetId, target.string("url")),
                hotValue = card.string("detail_text"),
                summary = target.string("excerpt"),
                imageUrl = imageUrl,
                label = card.objectOrNull("card_label")?.string("name").orEmpty()
            )
        }
    }

    private fun publicUrl(type: String, id: String, apiUrl: String): String {
        if (id.isNotEmpty()) {
            return when (type) {
                "question" -> "https://www.zhihu.com/question/$id"
                "article" -> "https://zhuanlan.zhihu.com/p/$id"
                else -> apiUrl
            }
        }
        return apiUrl
    }
}
