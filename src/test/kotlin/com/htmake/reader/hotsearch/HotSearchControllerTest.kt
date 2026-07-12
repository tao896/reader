package com.htmake.reader.hotsearch

import com.htmake.reader.api.controller.HotSearchController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HotSearchControllerTest {

    private val provider = object : HotSearchProvider {
        override fun platformConfig() = HotSearchPlatformConfig("test", "测试")
        override suspend fun fetchHotSearches() = HotSearchResult(emptyList(), "official", "测试")
    }
    private val providers = mapOf("test" to provider)

    @Test
    fun `valid request passes validation`() {
        assertNull(HotSearchController.validateRequest(providers, "test", null))
        assertNull(HotSearchController.validateRequest(providers, "test", "true"))
    }

    @Test
    fun `unknown platform is rejected`() {
        assertEquals(
            "无效的热搜平台: missing",
            HotSearchController.validateRequest(providers, "missing", null)
        )
    }

    @Test
    fun `invalid refresh value is rejected`() {
        assertEquals(
            "无效的刷新参数: yes",
            HotSearchController.validateRequest(providers, "test", "yes")
        )
    }

    @Test
    fun `weibo cookie is normalized without exposing its value`() {
        assertEquals(
            "SUB=secret; XSRF-TOKEN=token",
            HotSearchController.normalizeCookie("  SUB=secret; XSRF-TOKEN=token  ")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `weibo cookie must contain SUB credential`() {
        HotSearchController.normalizeCookie("XSRF-TOKEN=token")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `weibo cookie rejects header injection`() {
        HotSearchController.normalizeCookie("SUB=secret\r\nInjected=true")
    }
}
