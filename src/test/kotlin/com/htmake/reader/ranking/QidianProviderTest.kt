package com.htmake.reader.ranking

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class QidianProviderTest {

    private val provider = QidianProvider()

    @Test
    fun `siteConfig has correct site id and name`() {
        val config = provider.siteConfig()
        assertEquals("qidian", config.siteId)
        assertEquals("起点中文网", config.siteName)
        assertTrue(config.rankTypes.isNotEmpty())
        assertEquals("monthTicket", config.defaultRankType)
    }

    @Test
    fun `parseRankingPage extracts books correctly`() {
        val html = this::class.java.getResource("/ranking/qidian_monthly_ticket.html")!!.readText()
        val doc = Jsoup.parse(html)
        val books = provider.parseRankingPage(doc)

        assertEquals(3, books.size)

        val first = books[0]
        assertEquals(1, first.rank)
        assertEquals("1038862662", first.siteBookId)
        assertEquals("万相之王", first.name)
        assertEquals("天蚕土豆", first.author)
        assertEquals("玄幻", first.category)
        assertEquals("102345月票", first.metric)
        assertTrue(first.coverUrl.contains("1038862662"))
        assertTrue(first.officialUrl.contains("1038862662"))
        assertEquals("这是一个万相的世界", first.intro)
    }

    @Test
    fun `parseRankingPage handles protocol-relative URLs`() {
        val html = this::class.java.getResource("/ranking/qidian_monthly_ticket.html")!!.readText()
        val doc = Jsoup.parse(html)
        val books = provider.parseRankingPage(doc)

        assertTrue(books[0].coverUrl.startsWith("https://"))
        assertTrue(books[0].officialUrl.startsWith("https://"))
    }

    @Test
    fun `buildUrl produces valid URL for known rank types`() {
        val url = provider.buildUrl("monthTicket", 1)
        assertTrue(url.contains("qidian.com"))
        assertTrue(url.contains("page=1") || url.contains("/1"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buildUrl rejects unknown rank type`() {
        provider.buildUrl("INVALID_TYPE", 1)
    }
}
