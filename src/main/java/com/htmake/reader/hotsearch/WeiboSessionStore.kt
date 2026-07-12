package com.htmake.reader.hotsearch

import java.util.concurrent.ConcurrentHashMap

data class WeiboSessionStatus(
    val configured: Boolean,
    val expiresAt: Long = 0L
)

class WeiboSessionStore(
    private val ttlMs: Long = 12 * 60 * 60 * 1000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private data class Entry(
        val cookie: String,
        val expiresAt: Long
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    fun put(sessionId: String, cookie: String): WeiboSessionStatus {
        pruneExpired()
        val expiresAt = clock() + ttlMs
        entries[sessionId] = Entry(cookie, expiresAt)
        return WeiboSessionStatus(configured = true, expiresAt = expiresAt)
    }

    fun get(sessionId: String): String? {
        val entry = entries[sessionId] ?: return null
        if (entry.expiresAt <= clock()) {
            entries.remove(sessionId, entry)
            return null
        }
        return entry.cookie
    }

    fun status(sessionId: String): WeiboSessionStatus {
        val entry = entries[sessionId] ?: return WeiboSessionStatus(configured = false)
        if (entry.expiresAt <= clock()) {
            entries.remove(sessionId, entry)
            return WeiboSessionStatus(configured = false)
        }
        return WeiboSessionStatus(configured = true, expiresAt = entry.expiresAt)
    }

    fun remove(sessionId: String) {
        entries.remove(sessionId)
    }

    private fun pruneExpired() {
        val now = clock()
        entries.entries.forEach { entry ->
            if (entry.value.expiresAt <= now) {
                entries.remove(entry.key, entry.value)
            }
        }
    }
}
