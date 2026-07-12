package com.htmake.reader.ranking

import io.legado.app.help.http.okHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
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

    private val rankRouteMap = mapOf(
        "click" to "no-vip-click",
        "collect" to "favor",
        "recommend" to "recommend",
        "subscribe" to "buy",
        "monthTicket" to "yp",
        "tucao" to "tsukkomi",
        "newBook" to "yp_new",
        "blade" to "blade",
        "update" to "get-update-most"
    )

    private val periodRouteMap = mapOf(
        "weekly" to "week",
        "monthly" to "month",
        "total" to "total"
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
        val rankRoute = rankRouteMap[rankType]
            ?: throw IllegalArgumentException("Invalid rank type: $rankType")
        if (period != null && !periodMap.containsKey(period)) {
            throw IllegalArgumentException("Invalid period: $period")
        }
        val periodRoute = periodRouteMap[period ?: "weekly"]!!
        return "https://www.ciweimao.com/rank-index/$rankRoute-$periodRoute/$page"
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
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code} (${response.request.url})")
                }
                response.body?.string() ?: ""
            }
        }
        val doc = Jsoup.parse(html)
        val books = parseRankingPage(doc)
        if (books.isEmpty()) {
            throw IOException("刺猬猫榜单页面未解析到书籍，页面结构可能已更新")
        }
        return RankingResult(items = books, page = page, hasMore = books.size >= 10)
    }

    /**
     * 解析榜单页面 DOM，提取书籍列表
     */
    fun parseRankingPage(doc: Document): List<RankingBook> {
        // 第一项兼容旧版结构，第二项是当前 rank-index 页面结构。
        val items = doc.select(".rank-book-item, .rank-book-list > li[data-book-id]")
        val books = mutableListOf<RankingBook>()
        for ((index, item) in items.withIndex()) {
            val coverLink = item.selectFirst("a.cover")
            val name = item.select(".book-info h3").text().trim().ifEmpty {
                item.selectFirst(".cnt .tit a")?.text()?.trim().orEmpty()
            }
            if (name.isEmpty()) continue

            val bookId = item.attr("data-book-id").trim()
            val author = item.select(".book-info .author").text().trim().ifEmpty {
                item.selectFirst(".cnt > p a[href*=/reader/]")?.text()?.trim().orEmpty()
            }
            val category = item.select(".book-info .category").text().trim()
            val intro = item.select(".book-info .intro").text().trim().ifEmpty {
                item.selectFirst(".cnt .desc")?.text()?.trim().orEmpty()
            }
            val currentLatest = item.select(".cnt > p").firstOrNull {
                it.text().trim().startsWith("最近更新")
            }?.text()?.substringAfter("/")?.trim().orEmpty()
            val latestChapter = item.select(".book-info .latest-chapter").text().trim()
                .removePrefix("最新：").removePrefix("最新:").trim()
                .ifEmpty { currentLatest }

            val metricNum = item.select(".book-metric .num").text().trim()
            val metricUnit = item.select(".book-metric .unit").text().trim()
            val metric = if (metricNum.isNotEmpty()) "$metricNum$metricUnit" else ""

            val coverImage = item.selectFirst(".book-cover img, a.cover img")
            val coverUrl = normalizeUrl(
                coverImage?.attr("data-original")?.trim().orEmpty().ifEmpty {
                    coverImage?.attr("src")?.trim().orEmpty()
                }
            )

            val href = item.attr("href").trim().ifEmpty { coverLink?.attr("href")?.trim().orEmpty() }
            val officialUrl = when {
                href.startsWith("http") -> href
                href.isNotEmpty() -> "https://www.ciweimao.com$href"
                bookId.isNotEmpty() -> "https://www.ciweimao.com/book/$bookId"
                else -> ""
            }

            val rank = item.selectFirst(".rank-num, i.rank-top")
                ?.text()
                ?.trim()
                ?.toIntOrNull()
                ?: (index + 1)

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
