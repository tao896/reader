package com.htmake.reader.ranking

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class CiweimaoProviderTest {

    private val provider = CiweimaoProvider()

    @Test
    fun `siteConfig has correct site id and name`() {
        val config = provider.siteConfig()
        assertEquals("ciweimao", config.siteId)
        assertEquals("刺猬猫", config.siteName)
        assertTrue(config.rankTypes.isNotEmpty())
        assertEquals("click", config.defaultRankType)
    }

    @Test
    fun `siteConfig has period filter`() {
        val config = provider.siteConfig()
        val clickFilters = config.filters["click"]
        assertNotNull(clickFilters)
        val periodFilter = clickFilters!!.find { it.id == "period" }
        assertNotNull(periodFilter)
        assertEquals("weekly", periodFilter!!.defaultId)
        assertTrue(periodFilter.options.any { it.id == "monthly" })
        assertTrue(periodFilter.options.any { it.id == "total" })
    }

    @Test
    fun `parseRankingPage extracts books correctly`() {
        val html = this::class.java.getResource("/ranking/ciweimao_click_weekly.html")!!.readText()
        val doc = Jsoup.parse(html)
        val books = provider.parseRankingPage(doc)

        assertEquals(2, books.size)

        val first = books[0]
        assertEquals(1, first.rank)
        assertEquals("100234", first.siteBookId)
        assertEquals("灵境行者", first.name)
        assertEquals("卖报小郎君", first.author)
        assertEquals("都市异能", first.category)
        assertEquals("234567点击", first.metric)
        assertTrue(first.coverUrl.startsWith("https://"))
        assertEquals("灵气复苏的平行世界", first.intro)
        assertTrue(first.latestChapter.contains("终局"))
        assertTrue(first.officialUrl.contains("100234"))
    }

    @Test
    fun `parseRankingPage handles protocol-relative cover URLs`() {
        val html = this::class.java.getResource("/ranking/ciweimao_click_weekly.html")!!.readText()
        val doc = Jsoup.parse(html)
        val books = provider.parseRankingPage(doc)
        assertTrue(books.all { it.coverUrl.startsWith("https://") })
    }

    @Test
    fun `buildUrl produces valid URL for known rank type and period`() {
        val url = provider.buildUrl("click", "weekly", 1)
        assertTrue(url.contains("ciweimao.com"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buildUrl rejects unknown rank type`() {
        provider.buildUrl("INVALID", "weekly", 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buildUrl rejects unknown period`() {
        provider.buildUrl("click", "INVALID", 1)
    }
}
