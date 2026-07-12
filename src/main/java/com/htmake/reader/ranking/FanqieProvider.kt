package com.htmake.reader.ranking

import io.legado.app.help.http.okHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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
        val g = if (genderMap.containsKey(gender)) gender else "male"
        val c = if (categoryMap.containsKey(category)) category else "all"
        return "https://fanqienovel.com/rank/$rankType/$g/$c?page=$page"
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
        val items = doc.select(".rank-item")
        val books = mutableListOf<RankingBook>()
        for ((index, item) in items.withIndex()) {
            val name = item.select(".title").text().trim()
            if (name.isEmpty()) continue

            val bookId = item.attr("data-book-id").trim()
            val author = item.select(".author").text().trim()
                .removePrefix("作者：").removePrefix("作者:").trim()
            val intro = item.select(".intro").text().trim()
            val desc = item.select(".desc").text().trim()
            val category = desc.split("·").firstOrNull()?.trim() ?: ""
            val status = when {
                desc.contains("连载") -> "连载中"
                desc.contains("完结") -> "已完结"
                else -> ""
            }
            val coverUrl = item.select(".rank-book-cover img").attr("src").trim()
            val score = item.select(".score").text().trim()
            val metric = if (score.isNotEmpty()) "${score}分" else ""

            val rank = item.select(".rank-index span").text().trim().toIntOrNull() ?: (index + 1)
            val officialUrl = if (bookId.isNotEmpty()) "https://fanqienovel.com/page/$bookId" else ""

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
                    officialUrl = officialUrl
                )
            )
        }
        return books
    }
}
