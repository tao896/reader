package com.htmake.reader.ranking

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

data class CacheEntry(
    val result: RankingResult,
    val fetchedAt: Long,
    val stale: Boolean
)

class RankingCache(private val ttlMs: Long = 30 * 60 * 1000L) {

    private data class CachedValue(
        val result: RankingResult,
        val fetchedAt: Long
    )

    private val store = ConcurrentHashMap<String, CachedValue>()
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun getOrFetch(key: String, fetcher: suspend () -> RankingResult): CacheEntry {
        val cached = store[key]
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < ttlMs) {
            return CacheEntry(cached.result, cached.fetchedAt, stale = false)
        }

        val mutex = locks.getOrPut(key) { Mutex() }
        mutex.withLock {
            val rechecked = store[key]
            if (rechecked != null && System.currentTimeMillis() - rechecked.fetchedAt < ttlMs) {
                return CacheEntry(rechecked.result, rechecked.fetchedAt, stale = false)
            }

            try {
                val result = fetcher()
                val now = System.currentTimeMillis()
                store[key] = CachedValue(result, now)
                return CacheEntry(result, now, stale = false)
            } catch (e: Exception) {
                if (cached != null) {
                    return CacheEntry(cached.result, cached.fetchedAt, stale = true)
                }
                throw e
            }
        }
    }
}
