package com.htmake.reader.ranking

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class RankingCacheTest {

    private lateinit var cache: RankingCache

    @Before
    fun setup() {
        cache = RankingCache(ttlMs = 200)
    }

    @Test
    fun `cache hit returns same result without re-fetch`() = runBlocking {
        val fetchCount = AtomicInteger(0)
        val result = RankingResult(
            items = listOf(RankingBook(1, "id1", "Book1", "Author1")),
            page = 1,
            hasMore = false
        )

        val entry1 = cache.getOrFetch("key1") {
            fetchCount.incrementAndGet()
            result
        }
        val entry2 = cache.getOrFetch("key1") {
            fetchCount.incrementAndGet()
            result
        }

        assertEquals(1, fetchCount.get())
        assertEquals(result, entry1.result)
        assertEquals(result, entry2.result)
        assertFalse(entry1.stale)
    }

    @Test
    fun `cache expires after TTL`() = runBlocking {
        val fetchCount = AtomicInteger(0)
        val result = RankingResult(items = emptyList(), page = 1, hasMore = false)

        cache.getOrFetch("key1") {
            fetchCount.incrementAndGet()
            result
        }
        delay(250)
        cache.getOrFetch("key1") {
            fetchCount.incrementAndGet()
            result
        }

        assertEquals(2, fetchCount.get())
    }

    @Test
    fun `stale cache returned on fetch failure`() = runBlocking {
        val result = RankingResult(items = listOf(RankingBook(1, "id1", "Book1", "Author1")), page = 1, hasMore = false)

        cache.getOrFetch("key1") { result }
        delay(250)

        val entry = cache.getOrFetch("key1") {
            throw RuntimeException("network error")
        }

        assertEquals(result, entry.result)
        assertTrue(entry.stale)
    }

    @Test(expected = RuntimeException::class)
    fun `no stale data throws on fetch failure`(): Unit = runBlocking {
        cache.getOrFetch("new-key") {
            throw RuntimeException("network error")
        }
        Unit
    }

    @Test
    fun `concurrent requests are coalesced`() = runBlocking {
        val fetchCount = AtomicInteger(0)
        val result = RankingResult(items = emptyList(), page = 1, hasMore = false)

        val jobs = (1..5).map {
            async(Dispatchers.IO) {
                cache.getOrFetch("same-key") {
                    fetchCount.incrementAndGet()
                    delay(50)
                    result
                }
            }
        }
        jobs.forEach { it.await() }

        assertEquals(1, fetchCount.get())
    }
}
