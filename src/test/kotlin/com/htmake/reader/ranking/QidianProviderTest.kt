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
    fun `parseRankingPage supports current mobile SSR markup`() {
        val doc = Jsoup.parse(
            """
            <div class="y-list__item" data-index="0">
              <a href="//m.qidian.com/book/1040000001/">
                <img src="placeholder.png" data-src="//bookcover.yuewen.com/qdbimg/349573/1040000001/180">
                <div class="_ranking_hash">1</div>
                <h2>新版起点书籍</h2>
                <div class="_bookTitleR_hash">1.2万月票</div>
                <p class="_bookDesc_hash">新版简介</p>
                <p class="_subTitle_hash">新版作者 <em>·</em> 玄幻 <em>·</em> 100万字</p>
              </a>
            </div>
            """.trimIndent()
        )

        val book = provider.parseRankingPage(doc).single()
        assertEquals("1040000001", book.siteBookId)
        assertEquals("新版起点书籍", book.name)
        assertEquals("新版作者", book.author)
        assertEquals("玄幻", book.category)
        assertEquals("1.2万月票", book.metric)
        assertEquals("https://bookcover.yuewen.com/qdbimg/349573/1040000001/180", book.coverUrl)
    }

    @Test
    fun `buildUrl produces valid URL for known rank types`() {
        val url = provider.buildUrl("monthTicket", 1)
        assertEquals("https://m.qidian.com/rank/yuepiao/?page=1", url)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buildUrl rejects unknown rank type`() {
        provider.buildUrl("INVALID_TYPE", 1)
    }
}
