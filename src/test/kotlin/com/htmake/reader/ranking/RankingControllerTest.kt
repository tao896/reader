package com.htmake.reader.ranking

import com.htmake.reader.api.controller.RankingController
import org.junit.Assert.*
import org.junit.Test

class RankingControllerTest {

    private val providers = listOf(QidianProvider(), FanqieProvider(), CiweimaoProvider())
    private val providerMap = providers.associateBy { it.siteConfig().siteId }

    @Test
    fun `validateRequest accepts valid qidian params`() {
        val error = RankingController.validateRequest(providerMap, "qidian", "monthTicket", null, null, null, 1)
        assertNull(error)
    }

    @Test
    fun `validateRequest accepts valid fanqie params`() {
        val error = RankingController.validateRequest(providerMap, "fanqie", "read", "male", "all", null, 1)
        assertNull(error)
    }

    @Test
    fun `validateRequest accepts valid ciweimao params`() {
        val error = RankingController.validateRequest(providerMap, "ciweimao", "click", null, null, "weekly", 1)
        assertNull(error)
    }

    @Test
    fun `validateRequest rejects unknown site`() {
        val error = RankingController.validateRequest(providerMap, "unknown", "click", null, null, null, 1)
        assertNotNull(error)
        assertTrue(error!!.contains("站点"))
    }

    @Test
    fun `validateRequest rejects unknown rank type`() {
        val error = RankingController.validateRequest(providerMap, "qidian", "INVALID", null, null, null, 1)
        assertNotNull(error)
        assertTrue(error!!.contains("榜单"))
    }

    @Test
    fun `validateRequest rejects invalid page`() {
        val error = RankingController.validateRequest(providerMap, "qidian", "monthTicket", null, null, null, 0)
        assertNotNull(error)
        assertTrue(error!!.contains("页码"))
    }

    @Test
    fun `validateRequest rejects page over 50`() {
        val error = RankingController.validateRequest(providerMap, "qidian", "monthTicket", null, null, null, 51)
        assertNotNull(error)
        assertTrue(error!!.contains("页码"))
    }

    @Test
    fun `validateRequest rejects arbitrary URL in site field`() {
        val error = RankingController.validateRequest(providerMap, "http://evil.com", "click", null, null, null, 1)
        assertNotNull(error)
    }
}
