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
 * 番茄小说（fanqienovel.com）榜单数据源
 * 抓取番茄小说网页版榜单页面，支持按频道（男频/女频）与分类过滤
 */
class FanqieProvider : RankingProvider {

    companion object {
        private const val SITE_ID = "fanqie"
        private const val SITE_NAME = "番茄小说"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private val BOOK_ID_PATTERN = Regex("/page/(\\d+)")
    }

    private val rankTypeMap = linkedMapOf(
        "read" to "阅读榜",
        "newBook" to "新书榜"
    )

    private val genderMap = linkedMapOf(
        "male" to "男频",
        "female" to "女频"
    )

    private val categoryMap = linkedMapOf(
        "all" to "全部",
        "xuanhuan" to "玄幻",
        "dushi" to "都市",
        "lishi" to "历史",
        "kehuan" to "科幻",
        "wuxia" to "武侠",
        "yanqing" to "言情",
        "guzhuang" to "古装"
    )

    private val maleCategoryRouteMap = mapOf(
        "all" to "1141",
        "xuanhuan" to "258",
        "dushi" to "261",
        "lishi" to "273",
        "kehuan" to "8",
        "wuxia" to "1140",
        "yanqing" to "262",
        "guzhuang" to "272"
    )

    private val femaleCategoryRouteMap = mapOf(
        "all" to "1139",
        "xuanhuan" to "248",
        "dushi" to "267",
        "lishi" to "79",
        "kehuan" to "8",
        "wuxia" to "253",
        "yanqing" to "749",
        "guzhuang" to "246"
    )

    override fun siteConfig(): RankingSiteConfig {
        val genderFilter = RankingFilter(
            id = "gender",
            name = "频道",
            options = genderMap.map { (id, name) -> RankingOption(id, name) },
            defaultId = "male"
        )
        val categoryFilter = RankingFilter(
            id = "category",
            name = "分类",
            options = categoryMap.map { (id, name) -> RankingOption(id, name) },
            defaultId = "all"
        )
        val filtersPerRankType = rankTypeMap.keys.associateWith {
            listOf(genderFilter, categoryFilter)
        }
        return RankingSiteConfig(
            siteId = SITE_ID,
            siteName = SITE_NAME,
            rankTypes = rankTypeMap.map { (id, name) -> RankingOption(id, name) },
            defaultRankType = "read",
            filters = filtersPerRankType
        )
    }

    /**
     * 拼接榜单请求地址，仅支持 [rankTypeMap] 中登记的榜单类型；
     * gender/category 若未识别则回退到默认值（男频/全部）
     */
    fun buildUrl(rankType: String, gender: String?, category: String?, page: Int): String {
        if (!rankTypeMap.containsKey(rankType)) {
            throw IllegalArgumentException("Invalid rank type: $rankType")
        }
        val normalizedGender = if (genderMap.containsKey(gender)) gender!! else "male"
        val routeGender = if (normalizedGender == "male") "1" else "0"
        val routeRankType = if (rankType == "read") "2" else "1"
        val categoryRoutes = if (normalizedGender == "male") maleCategoryRouteMap else femaleCategoryRouteMap
        val normalizedCategory = category?.takeIf { categoryMap.containsKey(it) } ?: "all"
        val routeCategory = categoryRoutes[normalizedCategory] ?: categoryRoutes.getValue("all")

        // 番茄榜单的当前路由是 gender_rankMold_category。公开 SSR 页面只返回 Top 10，
        // page 参数保留在查询串中用于日志定位，但 Provider 不把它声明成可翻页数据。
        return "https://fanqienovel.com/rank/${routeGender}_${routeRankType}_$routeCategory?page=$page"
    }

    override suspend fun fetchRanking(
        rankType: String,
        gender: String?,
        category: String?,
        period: String?,
        page: Int
    ): RankingResult {
        val url = buildUrl(rankType, gender, category, page)
        val html = withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://fanqienovel.com/")
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
            throw IOException("番茄榜单页面未解析到书籍，页面结构可能已更新")
        }
        return RankingResult(items = books, page = page, hasMore = false)
    }

    /**
     * 解析榜单页面 DOM，提取书籍列表
     */
    fun parseRankingPage(doc: Document): List<RankingBook> {
        val items = doc.select(".rank-item, .rank-book-item")
        val books = mutableListOf<RankingBook>()
        for ((index, item) in items.withIndex()) {
            val name = item.select(".title").text().trim()
            if (name.isEmpty()) continue

            val bookLink = item.selectFirst("a[href^=/page/]")
            val bookId = item.attr("data-book-id").trim().ifEmpty {
                BOOK_ID_PATTERN.find(bookLink?.attr("href").orEmpty())?.groupValues?.get(1).orEmpty()
            }
            val author = item.selectFirst(".author a, .author")?.text()?.trim().orEmpty()
                .removePrefix("作者：").removePrefix("作者:").trim()
            val intro = item.selectFirst(".intro, .abstract")?.text()?.trim().orEmpty()
            val oldDesc = item.selectFirst(".desc:not(.abstract)")?.text()?.trim().orEmpty()
            val category = oldDesc.split("·").firstOrNull()?.trim().orEmpty()
            val status = when {
                item.select(".book-item-footer-status").text().contains("完结") || oldDesc.contains("完结") -> "已完结"
                item.select(".book-item-footer-status").text().contains("连载") || oldDesc.contains("连载") -> "连载中"
                else -> ""
            }
            val coverUrl = normalizeUrl(
                item.selectFirst(".rank-book-cover img, .book-cover-img")?.attr("src")?.trim().orEmpty()
            )
            val score = item.select(".score").text().trim()
            val metric = if (score.isNotEmpty()) "${score}分" else item.select(".book-item-count").text().trim()

            val rankText = item.selectFirst(".rank-index span, .book-item-index")?.text().orEmpty()
            val rank = rankText.filter { it.isDigit() }.toIntOrNull() ?: (index + 1)
            val officialUrl = if (bookId.isNotEmpty()) "https://fanqienovel.com/page/$bookId" else ""
            val latestChapter = item.select(".chapter").text().trim()
                .removePrefix("最近更新：").removePrefix("最近更新:").trim()

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
                    status = status,
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
