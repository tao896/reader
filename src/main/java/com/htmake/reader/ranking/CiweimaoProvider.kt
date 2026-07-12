package com.htmake.reader.ranking

import io.legado.app.help.http.okHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

/**
 * 刺猬猫小说（ciweimao.com）榜单数据源
 * 抓取榜单页面，支持点击/收藏/推荐等榜单类型与周期（周榜/月榜/总榜）过滤
 */
class CiweimaoProvider : RankingProvider {

    companion object {
        private const val SITE_ID = "ciweimao"
        private const val SITE_NAME = "刺猬猫"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    private val rankTypeMap = linkedMapOf(
        "click" to "点击榜",
        "collect" to "收藏榜",
        "recommend" to "推荐榜",
        "subscribe" to "订阅榜",
        "monthTicket" to "月票榜",
        "tucao" to "吐槽榜",
        "newBook" to "新书榜",
        "blade" to "刀片榜",
        "update" to "更新榜"
    )

    private val periodMap = linkedMapOf(
        "weekly" to "周榜",
        "monthly" to "月榜",
        "total" to "总榜"
    )

    override fun siteConfig(): RankingSiteConfig {
        val periodFilter = RankingFilter(
            id = "period",
            name = "周期",
            options = periodMap.map { (id, name) -> RankingOption(id, name) },
            defaultId = "weekly"
        )
        val filtersPerRankType = rankTypeMap.keys.associateWith {
            listOf(periodFilter)
        }
        return RankingSiteConfig(
            siteId = SITE_ID,
            siteName = SITE_NAME,
            rankTypes = rankTypeMap.map { (id, name) -> RankingOption(id, name) },
            defaultRankType = "click",
            filters = filtersPerRankType
        )
    }

    /**
     * 拼接榜单请求地址，仅支持 [rankTypeMap] / [periodMap] 中登记的榜单类型与周期
     */
    fun buildUrl(rankType: String, period: String?, page: Int): String {
        if (!rankTypeMap.containsKey(rankType)) {
            throw IllegalArgumentException("Invalid rank type: $rankType")
        }
        if (period != null && !periodMap.containsKey(period)) {
            throw IllegalArgumentException("Invalid period: $period")
        }
        val p = period ?: "weekly"
        return "https://www.ciweimao.com/rank/$rankType/$p?page=$page"
    }

    override suspend fun fetchRanking(
        rankType: String,
        gender: String?,
        category: String?,
        period: String?,
        page: Int
    ): RankingResult {
        val url = buildUrl(rankType, period, page)
        val html = withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://www.ciweimao.com/")
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
        val items = doc.select(".rank-book-item")
        val books = mutableListOf<RankingBook>()
        for ((index, item) in items.withIndex()) {
            val name = item.select(".book-info h3").text().trim()
            if (name.isEmpty()) continue

            val bookId = item.attr("data-book-id").trim()
            val author = item.select(".book-info .author").text().trim()
            val category = item.select(".book-info .category").text().trim()
            val intro = item.select(".book-info .intro").text().trim()
            val latestChapter = item.select(".book-info .latest-chapter").text().trim()
                .removePrefix("最新：").removePrefix("最新:").trim()

            val metricNum = item.select(".book-metric .num").text().trim()
            val metricUnit = item.select(".book-metric .unit").text().trim()
            val metric = if (metricNum.isNotEmpty()) "$metricNum$metricUnit" else ""

            val coverUrl = normalizeUrl(item.select(".book-cover img").attr("src").trim())

            val href = item.attr("href").trim()
            val officialUrl = when {
                href.startsWith("http") -> href
                href.isNotEmpty() -> "https://www.ciweimao.com$href"
                bookId.isNotEmpty() -> "https://www.ciweimao.com/book/$bookId"
                else -> ""
            }

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
                    latestChapter = latestChapter,
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
