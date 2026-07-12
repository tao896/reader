package com.htmake.reader.api.controller

import com.htmake.reader.api.ReturnData
import com.htmake.reader.ranking.*
import io.vertx.ext.web.RoutingContext
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

class RankingController(override val coroutineContext: CoroutineContext) : BaseController(coroutineContext) {

    private val providers: List<RankingProvider> = listOf(
        QidianProvider(),
        FanqieProvider(),
        CiweimaoProvider()
    )
    private val providerMap: Map<String, RankingProvider> = providers.associateBy { it.siteConfig().siteId }
    private val cache = RankingCache()

    suspend fun getBookRankingOptions(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        checkAuth(context)
        val options = providers.map { provider ->
            val config = provider.siteConfig()
            mapOf(
                "siteId" to config.siteId,
                "siteName" to config.siteName,
                "rankTypes" to config.rankTypes.map { mapOf("id" to it.id, "name" to it.name) },
                "defaultRankType" to config.defaultRankType,
                "filters" to config.filters.mapValues { (_, filters) ->
                    filters.map { filter ->
                        mapOf(
                            "id" to filter.id,
                            "name" to filter.name,
                            "options" to filter.options.map { mapOf("id" to it.id, "name" to it.name) },
                            "defaultId" to filter.defaultId
                        )
                    }
                }
            )
        }
        return returnData.setData(options)
    }

    suspend fun getBookRankings(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        checkAuth(context)

        val site = context.queryParam("site").firstOrNull() ?: ""
        val rankType = context.queryParam("rankType").firstOrNull() ?: ""
        val gender = context.queryParam("gender").firstOrNull()
        val category = context.queryParam("category").firstOrNull()
        val period = context.queryParam("period").firstOrNull()
        val page = context.queryParam("page").firstOrNull()?.toIntOrNull() ?: 1

        val validationError = validateRequest(providerMap, site, rankType, gender, category, period, page)
        if (validationError != null) {
            return returnData.setErrorMsg(validationError)
        }

        val provider = providerMap[site]!!
        val cacheKey = listOf(site, rankType, gender ?: "", category ?: "", period ?: "", page.toString()).joinToString("/")

        try {
            val entry = cache.getOrFetch(cacheKey) {
                provider.fetchRanking(rankType, gender, category, period, page)
            }
            val result = mapOf(
                "selection" to mapOf(
                    "site" to site,
                    "rankType" to rankType,
                    "gender" to gender,
                    "category" to category,
                    "period" to period
                ),
                "items" to entry.result.items.map { book ->
                    mapOf(
                        "rank" to book.rank,
                        "siteBookId" to book.siteBookId,
                        "name" to book.name,
                        "author" to book.author,
                        "coverUrl" to book.coverUrl,
                        "category" to book.category,
                        "metric" to book.metric,
                        "intro" to book.intro,
                        "status" to book.status,
                        "latestChapter" to book.latestChapter,
                        "officialUrl" to book.officialUrl
                    )
                },
                "page" to entry.result.page,
                "hasMore" to entry.result.hasMore,
                "fetchedAt" to entry.fetchedAt,
                "stale" to entry.stale
            )
            return returnData.setData(result)
        } catch (e: Exception) {
            logger.error("Ranking fetch failed for $site/$rankType: ${e.message}")
            return returnData.setErrorMsg("获取 ${providerMap[site]!!.siteConfig().siteName} 排行榜失败: ${e.message}")
        }
    }

    companion object {
        fun validateRequest(
            providerMap: Map<String, RankingProvider>,
            site: String,
            rankType: String,
            gender: String?,
            category: String?,
            period: String?,
            page: Int
        ): String? {
            if (!providerMap.containsKey(site)) {
                return "无效的站点: $site"
            }
            val config = providerMap[site]!!.siteConfig()
            if (config.rankTypes.none { it.id == rankType }) {
                return "无效的榜单类型: $rankType"
            }
            if (page < 1 || page > 50) {
                return "无效的页码: $page (1-50)"
            }
            val filters = config.filters[rankType]
            if (filters != null) {
                val genderFilter = filters.find { it.id == "gender" }
                if (gender != null && genderFilter != null && genderFilter.options.none { it.id == gender }) {
                    return "无效的性别筛选: $gender"
                }
                val categoryFilter = filters.find { it.id == "category" }
                if (category != null && categoryFilter != null && categoryFilter.options.none { it.id == category }) {
                    return "无效的分类筛选: $category"
                }
                val periodFilter = filters.find { it.id == "period" }
                if (period != null && periodFilter != null && periodFilter.options.none { it.id == period }) {
                    return "无效的周期筛选: $period"
                }
            }
            return null
        }
    }
}
