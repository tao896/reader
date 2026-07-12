package com.htmake.reader.ranking

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class FanqieProviderTest {

    private val provider = FanqieProvider()

    @Test
    fun `siteConfig has correct site id and name`() {
        val config = provider.siteConfig()
        assertEquals("fanqie", config.siteId)
        assertEquals("番茄小说", config.siteName)
        assertTrue(config.rankTypes.isNotEmpty())
        assertEquals("read", config.defaultRankType)
    }

    @Test
    fun `siteConfig has gender filter with male default`() {
        val config = provider.siteConfig()
        val genderFilters = config.filters["read"]
        assertNotNull(genderFilters)
        val genderFilter = genderFilters!!.find { it.id == "gender" }
        assertNotNull(genderFilter)
        assertEquals("male", genderFilter!!.defaultId)
    }

    @Test
    fun `parseRankingPage extracts books correctly`() {
        val html = this::class.java.getResource("/ranking/fanqie_male_read.html")!!.readText()
        val doc = Jsoup.parse(html)
        val books = provider.parseRankingPage(doc)

        assertEquals(2, books.size)

        val first = books[0]
        assertEquals(1, first.rank)
        assertEquals("7012345678", first.siteBookId)
        assertEquals("修仙归来当奶爸", first.name)
        assertEquals("柳下花前", first.author)
        assertEquals("都市修仙", first.category)
        assertTrue(first.coverUrl.contains("7012345678"))
        assertEquals("修仙万年归来发现有个女儿", first.intro)
    }

    @Test
    fun `parseRankingPage handles missing author prefix`() {
        val html = this::class.java.getResource("/ranking/fanqie_male_read.html")!!.readText()
        val doc = Jsoup.parse(html)
        val books = provider.parseRankingPage(doc)
        assertFalse(books[0].author.contains("作者："))
    }

    @Test
    fun `parseRankingPage supports current SSR markup`() {
        val doc = Jsoup.parse(
            """
            <div class="rank-book-item">
              <div class="book-item-index">01-</div>
              <img class="book-cover-img" src="//example.com/current-cover.jpg">
              <div class="title"><a href="/page/7600000000000000001">新版番茄书籍</a></div>
              <div class="author"><a>新版作者</a></div>
              <div class="desc abstract">新版简介</div>
              <span class="book-item-footer-status">连载中</span>
              <span class="book-item-count">在读：12.3万</span>
              <a class="chapter">最近更新：第100章</a>
            </div>
            """.trimIndent()
        )

        val book = provider.parseRankingPage(doc).single()
        assertEquals(1, book.rank)
        assertEquals("7600000000000000001", book.siteBookId)
        assertEquals("新版番茄书籍", book.name)
        assertEquals("新版作者", book.author)
        assertEquals("在读：12.3万", book.metric)
        assertEquals("第100章", book.latestChapter)
        assertEquals("https://example.com/current-cover.jpg", book.coverUrl)
    }

    @Test
    fun `buildUrl produces valid URL`() {
        val url = provider.buildUrl("read", "male", null, 1)
        assertEquals("https://fanqienovel.com/rank/1_2_1141?page=1", url)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buildUrl rejects unknown rank type`() {
        provider.buildUrl("INVALID", "male", null, 1)
    }
}
