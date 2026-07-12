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
    fun `parseRankingPage supports current rank-index markup`() {
        val doc = Jsoup.parse(
            """
            <ol class="rank-book-list">
              <li data-book-id="100499999">
                <i class="rank-top top1">1</i>
                <a class="cover" href="https://www.ciweimao.com/book/100499999" title="新版刺猬猫书籍">
                  <img src="placeholder.png" data-original="https://example.com/current-cover.jpg">
                </a>
                <div class="cnt">
                  <h3 class="tit"><a>新版刺猬猫书籍</a></h3>
                  <p>小说作者：<a href="https://www.ciweimao.com/reader/123">新版作者</a></p>
                  <p>最近更新：2026-07-12 / 第100章</p>
                  <p class="desc">新版简介</p>
                </div>
              </li>
            </ol>
            """.trimIndent()
        )

        val book = provider.parseRankingPage(doc).single()
        assertEquals(1, book.rank)
        assertEquals("100499999", book.siteBookId)
        assertEquals("新版刺猬猫书籍", book.name)
        assertEquals("新版作者", book.author)
        assertEquals("新版简介", book.intro)
        assertEquals("第100章", book.latestChapter)
        assertEquals("https://example.com/current-cover.jpg", book.coverUrl)
    }

    @Test
    fun `buildUrl produces valid URL for known rank type and period`() {
        val url = provider.buildUrl("click", "weekly", 1)
        assertEquals("https://www.ciweimao.com/rank-index/no-vip-click-week/1", url)
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
