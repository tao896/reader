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
    fun `buildUrl produces valid URL`() {
        val url = provider.buildUrl("read", "male", null, 1)
        assertTrue(url.contains("fanqienovel.com") || url.contains("changdunovel.com"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buildUrl rejects unknown rank type`() {
        provider.buildUrl("INVALID", "male", null, 1)
    }
}
