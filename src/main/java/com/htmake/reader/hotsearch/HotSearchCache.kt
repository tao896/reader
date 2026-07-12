package com.htmake.reader.hotsearch

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

data class HotSearchCacheEntry(
    val result: HotSearchResult,
    val fetchedAt: Long,
    val stale: Boolean
)

class HotSearchCache(private val ttlMs: Long = 5 * 60 * 1000L) {

    private data class CachedValue(
        val result: HotSearchResult,
        val fetchedAt: Long
    )

    private val store = ConcurrentHashMap<String, CachedValue>()
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun getOrFetch(
        key: String,
        forceRefresh: Boolean = false,
        fetcher: suspend () -> HotSearchResult
    ): HotSearchCacheEntry {
        val cached = store[key]
        if (!forceRefresh && cached != null && isFresh(cached)) {
            return HotSearchCacheEntry(cached.result, cached.fetchedAt, stale = false)
        }

        val mutex = locks.getOrPut(key) { Mutex() }
        mutex.withLock {
            val rechecked = store[key]
            if (!forceRefresh && rechecked != null && isFresh(rechecked)) {
                return HotSearchCacheEntry(rechecked.result, rechecked.fetchedAt, stale = false)
            }

            try {
                val result = fetcher()
                val now = System.currentTimeMillis()
                store[key] = CachedValue(result, now)
                return HotSearchCacheEntry(result, now, stale = false)
            } catch (e: Exception) {
                if (e is HotSearchAuthenticationException) {
                    throw e
                }
                val staleValue = rechecked ?: cached
                if (staleValue != null) {
                    return HotSearchCacheEntry(staleValue.result, staleValue.fetchedAt, stale = true)
                }
                throw e
            }
        }
    }

    private fun isFresh(value: CachedValue): Boolean {
        return System.currentTimeMillis() - value.fetchedAt < ttlMs
    }

    fun remove(key: String) {
        store.remove(key)
    }
}
