package com.htmake.reader.hotsearch

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class HotSearchCacheTest {

    private val result = HotSearchResult(
        listOf(HotSearchItem(1, "热搜", "https://example.com")),
        source = "official",
        sourceName = "官方"
    )

    @Test
    fun `fresh result is cached`() = runBlocking {
        val cache = HotSearchCache(ttlMs = 500)
        val calls = AtomicInteger(0)
        cache.getOrFetch("weibo") { calls.incrementAndGet(); result }
        val second = cache.getOrFetch("weibo") { calls.incrementAndGet(); result }

        assertEquals(1, calls.get())
        assertFalse(second.stale)
    }

    @Test
    fun `force refresh bypasses fresh cache`() = runBlocking {
        val cache = HotSearchCache(ttlMs = 500)
        val calls = AtomicInteger(0)
        cache.getOrFetch("weibo") { calls.incrementAndGet(); result }
        cache.getOrFetch("weibo", forceRefresh = true) { calls.incrementAndGet(); result }

        assertEquals(2, calls.get())
    }

    @Test
    fun `expired value is returned as stale when refresh fails`() = runBlocking {
        val cache = HotSearchCache(ttlMs = 10)
        cache.getOrFetch("weibo") { result }
        delay(20)

        val entry = cache.getOrFetch("weibo") { throw IllegalStateException("network") }
        assertEquals(result, entry.result)
        assertTrue(entry.stale)
    }

    @Test(expected = HotSearchAuthenticationException::class)
    fun `authentication failure never falls back to stale cache`(): Unit = runBlocking {
        val cache = HotSearchCache(ttlMs = 10)
        cache.getOrFetch("weibo/session") { result }
        delay(20)

        cache.getOrFetch("weibo/session") {
            throw HotSearchAuthenticationException("expired")
        }
        Unit
    }
}
