package com.htmake.reader.hotsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeiboSessionStoreTest {

    @Test
    fun `cookie is isolated by reader session`() {
        var now = 1000L
        val store = WeiboSessionStore(ttlMs = 500L) { now }

        val status = store.put("session-a", "SUB=secret")

        assertTrue(status.configured)
        assertEquals(1500L, status.expiresAt)
        assertEquals("SUB=secret", store.get("session-a"))
        assertNull(store.get("session-b"))
    }

    @Test
    fun `cookie expires and is removed from memory`() {
        var now = 1000L
        val store = WeiboSessionStore(ttlMs = 500L) { now }
        store.put("session-a", "SUB=secret")

        now = 1500L

        assertNull(store.get("session-a"))
        assertFalse(store.status("session-a").configured)
    }

    @Test
    fun `cookie can be cleared explicitly`() {
        val store = WeiboSessionStore()
        store.put("session-a", "SUB=secret")

        store.remove("session-a")

        assertNull(store.get("session-a"))
    }
}
