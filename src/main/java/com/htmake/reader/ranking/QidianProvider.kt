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
        private val BOOK_ID_PATTERN = Regex("/book/(\\d+)")
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

    /**
     * 起点对外展示的榜单标识与阅读 Web 端曾经使用的 camelCase 标识不同。
     * 前端继续使用稳定的内部标识，由 Provider 在这里转换为当前真实路由。
     */
    private val rankRouteMap = mapOf(
        "monthTicket" to "yuepiao",
        "bestSell" to "hotsales",
        "readIndex" to "readindex",
        "newBook" to "newbook",
        "recom" to "rec",
        "update" to "update",
        "signNewBook" to "sign",
        "newAuthor" to "newauthor"
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
        val route = rankRouteMap[rankType]
            ?: throw IllegalArgumentException("Invalid rank type: $rankType")
        return "https://m.qidian.com/rank/$route/?page=$page"
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
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code} (${response.request.url})")
                }
                response.body?.string() ?: ""
            }
        }
        val doc = Jsoup.parse(html)
        val books = parseRankingPage(doc)
        if (books.isEmpty()) {
            throw IOException("起点榜单页面未解析到书籍，页面结构可能已更新")
        }
        // 当前移动榜单公开展示 Top 20，没有可稳定使用的服务端翻页地址。
        return RankingResult(items = books, page = page, hasMore = false)
    }

    /**
     * 解析榜单页面 DOM，提取书籍列表
     */
    fun parseRankingPage(doc: Document): List<RankingBook> {
        // 第一项兼容旧版页面和测试夹具，第二项是当前移动站 SSR 结构。
        val items = doc.select(".book-list .book-item, .y-list__item")
        val books = mutableListOf<RankingBook>()
        for ((index, item) in items.withIndex()) {
            val bookLink = item.selectFirst("a[href*=/book/]")
            val name = item.selectFirst(".book-mid-info h2, h2")?.text()?.trim().orEmpty()
            if (name.isEmpty()) continue

            val href = bookLink?.attr("href")?.trim().orEmpty()
            val bookId = item.attr("data-bookid").trim().ifEmpty {
                BOOK_ID_PATTERN.find(href)?.groupValues?.get(1).orEmpty()
            }
            val subtitleParts = item.selectFirst("p[class*=_subTitle_]")
                ?.text()
                ?.split("·")
                ?.map { it.trim() }
                .orEmpty()
            val author = item.select(".author a").text().trim().ifEmpty {
                subtitleParts.getOrNull(0).orEmpty()
            }
            val intro = item.selectFirst(".intro, p[class*=_bookDesc_]")?.text()?.trim().orEmpty()
            val category = item.select(".tag span").first()?.text()?.trim().orEmpty().ifEmpty {
                subtitleParts.getOrNull(1).orEmpty()
            }

            val oldMetricValue = item.select(".book-right-info .total span").text().trim()
            val oldMetricUnit = item.select(".book-right-info .total em").text().trim()
            val metric = if (oldMetricValue.isNotEmpty()) {
                "$oldMetricValue$oldMetricUnit"
            } else {
                item.selectFirst("div[class*=_bookTitleR_]")?.text()?.trim().orEmpty()
            }

            val coverImage = item.selectFirst(".book-img-box img, img")
            val coverUrl = normalizeUrl(
                coverImage?.attr("data-src")?.trim().orEmpty().ifEmpty {
                    coverImage?.attr("src")?.trim().orEmpty()
                }
            )
            val officialUrl = normalizeUrl(
                item.selectFirst(".book-img-box a")?.attr("href")?.trim().orEmpty().ifEmpty { href }
            )

            val rank = item.selectFirst(".rank-num, div[class*=_ranking_]")
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
