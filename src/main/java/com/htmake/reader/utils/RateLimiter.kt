package com.htmake.reader.utils

import java.util.concurrent.ConcurrentHashMap

class RateLimiter(
    private val maxAttempts: Int = 10,
    private val windowMillis: Long = 5 * 60 * 1000
) {
    private val attempts = ConcurrentHashMap<String, MutableList<Long>>()

    fun isAllowed(key: String): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = attempts.compute(key) { _, existing ->
            val list = existing ?: mutableListOf()
            list.removeAll { now - it > windowMillis }
            list
        }!!
        synchronized(timestamps) {
            if (timestamps.size >= maxAttempts) return false
            timestamps.add(now)
        }
        return true
    }

    fun cleanup() {
        val now = System.currentTimeMillis()
        attempts.entries.removeIf { entry ->
            synchronized(entry.value) {
                entry.value.removeAll { now - it > windowMillis }
                entry.value.isEmpty()
            }
        }
    }
}
