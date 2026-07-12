package com.htmake.reader.hotsearch

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mu.KotlinLogging
import java.io.IOException
import java.net.URLEncoder

private val logger = KotlinLogging.logger {}

class WeiboHotSearchProvider : HotSearchProvider {

    companion object {
        private const val OFFICIAL_URL = "https://weibo.com/ajax/side/hotSearch"
        private const val PERSONALIZED_URL = "https://weibo.com/ajax/statuses/mineBand"
        private const val FALLBACK_URL = "https://v2.xxapi.cn/api/weibohot"
    }

    override fun platformConfig() = HotSearchPlatformConfig("weibo", "微博")

    suspend fun fetchPersonalized(cookie: String): HotSearchResult {
        val json = HotSearchHttp.get(
            PERSONALIZED_URL,
            mapOf(
                "Referer" to "https://weibo.com/hot/mine",
                "Cookie" to cookie
            )
        )
        val root = JsonParser().parse(json).asJsonObject
        if (root.intOrNull("ok") != 1) {
            throw HotSearchAuthenticationException("微博登录已失效，请重新配置")
        }
        val items = parsePersonalized(json)
        if (items.isEmpty()) {
            throw IOException("微博“我的”热搜未返回数据")
        }
        return HotSearchResult(items, source = "personalized", sourceName = "微博·我的")
    }

    override suspend fun fetchHotSearches(): HotSearchResult {
        try {
            val items = parseOfficial(HotSearchHttp.get(OFFICIAL_URL, mapOf("Referer" to "https://weibo.com/")))
            if (items.isNotEmpty()) {
                return HotSearchResult(items, source = "official", sourceName = "微博官方")
            }
            throw IOException("微博官方接口未返回热搜数据")
        } catch (officialError: Exception) {
            logger.warn("Weibo official hot search unavailable, using fallback: ${officialError.message}")
            val items = parseAggregated(HotSearchHttp.get(FALLBACK_URL))
            if (items.isEmpty()) {
                throw IOException("微博官方接口和聚合源均未返回热搜数据", officialError)
            }
            return HotSearchResult(items, source = "aggregated", sourceName = "XXAPI 聚合")
        }
    }

    fun parseOfficial(json: String): List<HotSearchItem> {
        val root = JsonParser().parse(json).asJsonObject
        val data = root.objectOrNull("data") ?: return emptyList()
        val realtime = data.arrayOrNull("realtime") ?: return emptyList()
        return realtime.mapIndexedNotNull { index, element ->
            val item = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapIndexedNotNull null
            val title = item.string("note").ifEmpty { item.string("word") }
            if (title.isEmpty()) return@mapIndexedNotNull null
            val query = item.string("word_scheme").ifEmpty { "#$title#" }
            HotSearchItem(
                rank = index + 1,
                title = title,
                url = weiboSearchUrl(query),
                hotValue = formatOfficialHotValue(item),
                label = item.string("label_name").ifEmpty { item.string("flag_desc") }
            )
        }
    }

    fun parsePersonalized(json: String): List<HotSearchItem> {
        val root = JsonParser().parse(json).asJsonObject
        val realtime = root.objectOrNull("data")?.arrayOrNull("realtime") ?: return emptyList()
        return realtime.mapIndexedNotNull { _, element ->
            val item = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapIndexedNotNull null
            val rank = item.intOrNull("rank") ?: return@mapIndexedNotNull null
            val title = item.string("note").ifEmpty { item.string("word") }
            if (title.isEmpty()) return@mapIndexedNotNull null
            val query = item.string("word_scheme").ifEmpty { title }
            HotSearchItem(
                rank = rank + 1,
                title = title,
                url = weiboSearchUrl(query),
                hotValue = item.string("description"),
                label = item.string("icon_desc").ifEmpty { item.string("small_icon_desc") }
            )
        }
    }

    fun parseAggregated(json: String): List<HotSearchItem> {
        val root = JsonParser().parse(json).asJsonObject
        val data = root.arrayOrNull("data") ?: return emptyList()
        return data.mapIndexedNotNull { index, element ->
            val item = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapIndexedNotNull null
            val title = item.string("title")
            if (title.isEmpty()) return@mapIndexedNotNull null
            HotSearchItem(
                rank = item.intOrNull("index") ?: (index + 1),
                title = title,
                url = item.string("url").ifEmpty { weiboSearchUrl("#$title#") },
                hotValue = item.string("hot")
            )
        }
    }

    private fun formatOfficialHotValue(item: JsonObject): String {
        val number = item.longOrNull("num") ?: return item.string("hot")
        return when {
            number >= 10000L -> "${number / 10000L}万"
            number > 0L -> number.toString()
            else -> ""
        }
    }

    private fun weiboSearchUrl(query: String): String {
        return "https://s.weibo.com/weibo?q=${URLEncoder.encode(query, "UTF-8")}&t=547"
    }
}

internal fun JsonObject.string(name: String): String {
    val value = get(name) ?: return ""
    return if (value.isJsonNull || !value.isJsonPrimitive) "" else value.asString.trim()
}

internal fun JsonObject.intOrNull(name: String): Int? {
    val value = get(name) ?: return null
    return try {
        if (value.isJsonNull || !value.isJsonPrimitive) null else value.asInt
    } catch (_: Exception) {
        null
    }
}

internal fun JsonObject.longOrNull(name: String): Long? {
    val value = get(name) ?: return null
    return try {
        if (value.isJsonNull || !value.isJsonPrimitive) null else value.asLong
    } catch (_: Exception) {
        null
    }
}

internal fun JsonObject.objectOrNull(name: String): JsonObject? {
    val value = get(name) ?: return null
    return if (value.isJsonObject) value.asJsonObject else null
}

internal fun JsonObject.arrayOrNull(name: String) = get(name)?.takeIf { it.isJsonArray }?.asJsonArray
