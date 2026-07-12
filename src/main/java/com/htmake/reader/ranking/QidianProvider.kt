package com.htmake.reader.ranking

import io.legado.app.help.http.okHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

/**
 * 起点中文网（qidian.com）榜单数据源
 * 抓取起点触屏版（m.qidian.com）榜单页面
 */
class QidianProvider : RankingProvider {

    companion object {
        private const val SITE_ID = "qidian"
        private const val SITE_NAME = "起点中文网"
        private const val USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"
    }

    private val rankTypeMap = linkedMapOf(
        "monthTicket" to "月票榜",
        "bestSell" to "畅销榜",
        "readIndex" to "阅读榜",
        "newBook" to "新书榜",
        "recom" to "推荐榜",
        "update" to "更新榜",
        "signNewBook" to "签约新书榜",
        "newAuthor" to "新人榜"
    )

    override fun siteConfig(): RankingSiteConfig {
        return RankingSiteConfig(
            siteId = SITE_ID,
            siteName = SITE_NAME,
            rankTypes = rankTypeMap.map { (id, name) -> RankingOption(id, name) },
            defaultRankType = "monthTicket",
            filters = emptyMap()
        )
    }

    /**
     * 拼接榜单请求地址，仅支持 [rankTypeMap] 中登记的榜单类型
     */
    fun buildUrl(rankType: String, page: Int): String {
        if (!rankTypeMap.containsKey(rankType)) {
            throw IllegalArgumentException("Invalid rank type: $rankType")
        }
        return "https://m.qidian.com/rank/$rankType/?page=$page"
    }

    override suspend fun fetchRanking(
        rankType: String,
        gender: String?,
        category: String?,
        period: String?,
        page: Int
    ): RankingResult {
        val url = buildUrl(rankType, page)
        val html = withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://m.qidian.com/")
                .build()
            val client = okHttpClient.newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            client.newCall(request).execute().use { response ->
                response.body?.string() ?: ""
            }
        }
        val doc = Jsoup.parse(html)
        val books = parseRankingPage(doc)
        return RankingResult(items = books, page = page, hasMore = books.size >= 20)
    }

    /**
     * 解析榜单页面 DOM，提取书籍列表
     */
    fun parseRankingPage(doc: Document): List<RankingBook> {
        val items = doc.select(".book-list .book-item")
        val books = mutableListOf<RankingBook>()
        for ((index, item) in items.withIndex()) {
            val name = item.select(".book-mid-info h2").text().trim()
            if (name.isEmpty()) continue

            val bookId = item.attr("data-bookid").trim()
            val author = item.select(".author a").text().trim()
            val intro = item.select(".intro").text().trim()
            val category = item.select(".tag span").first()?.text()?.trim() ?: ""

            val metricValue = item.select(".book-right-info .total span").text().trim()
            val metricUnit = item.select(".book-right-info .total em").text().trim()
            val metric = if (metricValue.isNotEmpty()) "$metricValue$metricUnit" else ""

            val coverUrl = normalizeUrl(item.select(".book-img-box img").attr("src").trim())
            val officialUrl = normalizeUrl(item.select(".book-img-box a").attr("href").trim())

            val rank = item.select(".rank-num").text().trim().toIntOrNull() ?: (index + 1)

            books.add(
                RankingBook(
                    rank = rank,
                    siteBookId = bookId,
                    name = name,
                    author = author,
                    coverUrl = coverUrl,
                    category = category,
                    metric = metric,
                    intro = intro,
                    officialUrl = officialUrl
                )
            )
        }
        return books
    }

    private fun normalizeUrl(url: String): String {
        return if (url.startsWith("//")) "https:$url" else url
    }
}
