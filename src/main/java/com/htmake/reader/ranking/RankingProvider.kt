package com.htmake.reader.ranking

data class RankingBook(
    val rank: Int,
    val siteBookId: String,
    val name: String,
    val author: String,
    val coverUrl: String = "",
    val category: String = "",
    val metric: String = "",
    val intro: String = "",
    val status: String = "",
    val latestChapter: String = "",
    val officialUrl: String = ""
)

data class RankingOption(
    val id: String,
    val name: String
)

data class RankingFilter(
    val id: String,
    val name: String,
    val options: List<RankingOption>,
    val defaultId: String
)

data class RankingSiteConfig(
    val siteId: String,
    val siteName: String,
    val rankTypes: List<RankingOption>,
    val defaultRankType: String,
    val filters: Map<String, List<RankingFilter>>
)

data class RankingResult(
    val items: List<RankingBook>,
    val page: Int,
    val hasMore: Boolean
)

interface RankingProvider {
    fun siteConfig(): RankingSiteConfig
    suspend fun fetchRanking(
        rankType: String,
        gender: String?,
        category: String?,
        period: String?,
        page: Int
    ): RankingResult
}
