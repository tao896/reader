package com.htmake.reader.verticle

import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RestVerticleSessionTest {

    private lateinit var vertx: Vertx
    private var port: Int = 0

    @Before
    fun startServer() {
        port = ServerSocket(0).use { it.localPort }
        vertx = Vertx.vertx()
        val deployed = CountDownLatch(1)
        val listening = CountDownLatch(1)
        var deploymentError: Throwable? = null
        val testVerticle = object : RestVerticle() {
            override suspend fun initRouter(router: Router) {
                router.get("/reader3/session-id").handler {
                    it.response().end(it.session().id())
                }
            }

            override fun started() {
                listening.countDown()
            }
        }.apply {
            port = this@RestVerticleSessionTest.port
        }

        vertx.deployVerticle(testVerticle) { result ->
            if (result.failed()) {
                deploymentError = result.cause()
            }
            deployed.countDown()
        }

        if (!deployed.await(10, TimeUnit.SECONDS)) {
            throw AssertionError("Timed out while starting test server")
        }
        deploymentError?.let { throw AssertionError("Failed to start test server", it) }
        if (!listening.await(10, TimeUnit.SECONDS)) {
            throw AssertionError("Test server did not start listening")
        }
    }

    @After
    fun stopServer() {
        val stopped = CountDownLatch(1)
        vertx.close { stopped.countDown() }
        stopped.await(10, TimeUnit.SECONDS)
    }

    @Test
    fun `cors preflight does not create or clear reader session cookie`() {
        val connection = openConnection("/reader3/session-id").apply {
            requestMethod = "OPTIONS"
            setRequestProperty("Origin", "http://localhost:3000")
            setRequestProperty("Access-Control-Request-Method", "POST")
            setRequestProperty("Access-Control-Request-Headers", "content-type")
        }

        assertEquals(200, connection.responseCode)
        assertNull(connection.getHeaderField("Set-Cookie"))
        connection.inputStream.use { it.readBytes() }
        connection.disconnect()
    }

    @Test
    fun `reader session remains stable across requests`() {
        val first = openConnection("/reader3/session-id")
        assertEquals(200, first.responseCode)
        val firstSessionId = first.inputStream.bufferedReader().use { it.readText() }
        val setCookie = first.getHeaderField("Set-Cookie")
        assertNotNull(setCookie)
        first.disconnect()

        val cookie = requireNotNull(setCookie).substringBefore(';')
        val second = openConnection("/reader3/session-id").apply {
            setRequestProperty("Cookie", cookie)
        }
        assertEquals(200, second.responseCode)
        val secondSessionId = second.inputStream.bufferedReader().use { it.readText() }
        second.disconnect()

        assertEquals(firstSessionId, secondSessionId)
    }

    private fun openConnection(path: String): HttpURLConnection {
        return URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
    }
}
