package com.htmake.reader.api.controller

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookProgressTest {

    @Test
    fun `legacy same-chapter update preserves position`() {
        val book = Book(
            durChapterIndex = 3,
            durChapterPos = 42,
            durChapterPositionType = BOOK_PROGRESS_TEXT_OFFSET,
            durChapterTime = 100
        )

        applyBookProgress(book, BookChapter(index = 3, title = "chapter 3"), null, null, 200)

        assertEquals(3, book.durChapterIndex)
        assertEquals(42, book.durChapterPos)
        assertEquals(BOOK_PROGRESS_TEXT_OFFSET, book.durChapterPositionType)
        assertEquals(200, book.durChapterTime)
    }

    @Test
    fun `legacy chapter change resets position`() {
        val book = Book(
            durChapterIndex = 3,
            durChapterPos = 42,
            durChapterPositionType = BOOK_PROGRESS_TEXT_OFFSET,
            durChapterTime = 100
        )

        applyBookProgress(book, BookChapter(index = 4, title = "chapter 4"), null, null, 200)

        assertEquals(4, book.durChapterIndex)
        assertEquals("chapter 4", book.durChapterTitle)
        assertEquals(0, book.durChapterPos)
        assertNull(book.durChapterPositionType)
    }

    @Test
    fun `explicit zero position overwrites existing progress`() {
        val book = Book(
            durChapterIndex = 3,
            durChapterPos = 42,
            durChapterPositionType = BOOK_PROGRESS_TEXT_OFFSET,
            durChapterTime = 100
        )

        applyBookProgress(
            book,
            BookChapter(index = 0, title = "first chapter"),
            0,
            BOOK_PROGRESS_TEXT_OFFSET,
            200
        )

        assertEquals(0, book.durChapterIndex)
        assertEquals(0, book.durChapterPos)
        assertEquals(BOOK_PROGRESS_TEXT_OFFSET, book.durChapterPositionType)
    }

    @Test
    fun `position validation supports the three wire types`() {
        assertTrue(isValidBookProgressPosition(0, BOOK_PROGRESS_TEXT_OFFSET))
        assertTrue(isValidBookProgressPosition(Int.MAX_VALUE, BOOK_PROGRESS_TEXT_OFFSET))
        assertTrue(isValidBookProgressPosition(BOOK_PROGRESS_MAX_CHAPTER_RATIO, BOOK_PROGRESS_CHAPTER_RATIO))
        assertTrue(isValidBookProgressPosition(3600, BOOK_PROGRESS_AUDIO_SECONDS))

        assertFalse(isValidBookProgressPosition(-1, BOOK_PROGRESS_TEXT_OFFSET))
        assertFalse(isValidBookProgressPosition(BOOK_PROGRESS_MAX_CHAPTER_RATIO + 1, BOOK_PROGRESS_CHAPTER_RATIO))
        assertFalse(isValidBookProgressPosition(-1, BOOK_PROGRESS_AUDIO_SECONDS))
        assertFalse(isValidBookProgressPosition(0, "pixels"))
    }

    @Test
    fun `server progress timestamps are strictly ordered`() {
        val first = nextBookProgressTime("ordered-book", now = 100)
        val second = nextBookProgressTime("ordered-book", now = 100)

        assertTrue(second > first)
    }

    @Test
    fun `book progress clocks are isolated by user and book`() {
        val now = 1_000_000L
        val future = now + 86_400_000
        val futureBookTime = nextBookProgressTime(
            "user-a\u0000book-a",
            persistedTime = future,
            now = now
        )
        val otherBookTime = nextBookProgressTime(
            "user-b\u0000book-b",
            now = now
        )

        assertTrue(futureBookTime > future)
        assertEquals(now, otherBookTime)
    }

    @Test
    fun `webdav progress file keeps the newest timestamp`() {
        assertFalse(shouldStoreBookProgressFile(200, 199))
        assertTrue(shouldStoreBookProgressFile(200, 200))
        assertTrue(shouldStoreBookProgressFile(200, 201))
        assertTrue(shouldStoreBookProgressFile(null, 201))
        assertFalse(shouldStoreBookProgressFile(200, null))
        assertFalse(shouldStoreBookProgressFile(null, null))
    }

    @Test
    fun `webdav progress requires an explicit valid timestamp`() {
        assertNull(parseBookProgressTime(null))
        assertNull(parseBookProgressTime("invalid"))
        assertNull(parseBookProgressTime(-1))
        assertNull(parseBookProgressTime(Long.MAX_VALUE))
        assertNull(parseBookProgressTime(Long.MAX_VALUE - 1))
        assertEquals(0L, parseBookProgressTime(0))
        assertEquals(123L, parseBookProgressTime("123"))
    }

    @Test
    fun `imported timestamps use a stricter future window than stored timestamps`() {
        val now = 1_000_000L
        val oneDay = 24L * 60 * 60 * 1000
        val importBoundary = now + BOOK_PROGRESS_MAX_IMPORT_FUTURE_DRIFT
        val storedBoundary = now + BOOK_PROGRESS_MAX_FUTURE_DRIFT

        assertEquals(now + oneDay, parseImportedBookProgressTime(now + oneDay, now))
        assertEquals(importBoundary, parseImportedBookProgressTime(importBoundary, now))
        assertNull(parseImportedBookProgressTime(importBoundary + 1, now))
        assertEquals(
            importBoundary + 1,
            parsePlausibleBookProgressTime(importBoundary + 1, now)
        )
        assertEquals(storedBoundary, parsePlausibleBookProgressTime(storedBoundary, now))
        assertNull(parsePlausibleBookProgressTime(storedBoundary + 1, now))
        assertNull(parseImportedBookProgressTime(Long.MAX_VALUE, now))
        assertNull(parseImportedBookProgressTime(Long.MAX_VALUE - 1, now))
    }

    @Test
    fun `server can advance an imported boundary without invalidating its own time`() {
        val now = 1_000_000L
        val importedBoundary = now + BOOK_PROGRESS_MAX_IMPORT_FUTURE_DRIFT
        val serverTime = nextBookProgressTime(
            "import-boundary-book",
            persistedTime = importedBoundary,
            now = now
        )

        assertEquals(importedBoundary + 1, serverTime)
        assertNull(parseImportedBookProgressTime(serverTime, now))
        assertEquals(serverTime, parsePlausibleBookProgressTime(serverTime, now))
    }

    @Test
    fun `webdav apply rejects a timestamp only valid for stored server data`() {
        val now = System.currentTimeMillis()
        val storedOnlyTime =
            now + BOOK_PROGRESS_MAX_IMPORT_FUTURE_DRIFT + 6L * 60 * 60 * 1000
        val existing = Book(durChapterIndex = 3, durChapterTime = now)
        val incoming = Book(
            durChapterIndex = 4,
            durChapterPos = 10,
            durChapterPositionType = BOOK_PROGRESS_TEXT_OFFSET,
            durChapterTime = storedOnlyTime
        )

        assertTrue(isPlausibleBookProgressTime(storedOnlyTime))
        assertNull(parseImportedBookProgressTime(storedOnlyTime))
        assertFalse(applyBookProgressFromWebdav(existing, incoming))
        assertEquals(3, existing.durChapterIndex)
        assertEquals(now, existing.durChapterTime)
    }

    @Test
    fun `webdav apply treats the wider server timestamp window as existing data`() {
        val now = System.currentTimeMillis()
        val storedOnlyTime =
            now + BOOK_PROGRESS_MAX_IMPORT_FUTURE_DRIFT + 6L * 60 * 60 * 1000
        val incomingTime = now + 24L * 60 * 60 * 1000
        val existing = Book(durChapterIndex = 8, durChapterTime = storedOnlyTime)
        val incoming = Book(
            durChapterIndex = 2,
            durChapterPos = 20,
            durChapterPositionType = BOOK_PROGRESS_TEXT_OFFSET,
            durChapterTime = incomingTime
        )

        assertEquals(incomingTime, parseImportedBookProgressTime(incomingTime))
        assertFalse(applyBookProgressFromWebdav(existing, incoming))
        assertEquals(8, existing.durChapterIndex)
        assertEquals(storedOnlyTime, existing.durChapterTime)
    }

    @Test
    fun `near-long-max persisted data cannot saturate a book clock`() {
        val first = nextBookProgressTime(
            "saturated-book",
            persistedTime = Long.MAX_VALUE - 1,
            now = 100
        )
        val second = nextBookProgressTime(
            "saturated-book",
            persistedTime = Long.MAX_VALUE - 1,
            now = 100
        )

        assertEquals(100, first)
        assertEquals(101, second)
    }

    @Test
    fun `valid webdav progress can repair an invalid saturated shelf time`() {
        val existing = Book(
            durChapterIndex = 9,
            durChapterTime = Long.MAX_VALUE
        )
        val incoming = Book(
            durChapterIndex = 1,
            durChapterPos = 20,
            durChapterPositionType = BOOK_PROGRESS_TEXT_OFFSET,
            durChapterTime = 200
        )

        assertTrue(applyBookProgressFromWebdav(existing, incoming))
        assertEquals(1, existing.durChapterIndex)
        assertEquals(200, existing.durChapterTime)
    }

    @Test
    fun `a future webdav timestamp cannot freeze later server progress`() {
        val futureTime = System.currentTimeMillis() + 86_400_000
        val existing = Book(durChapterTime = 100)
        val incoming = Book(
            durChapterIndex = 2,
            durChapterPos = 30,
            durChapterPositionType = BOOK_PROGRESS_TEXT_OFFSET,
            durChapterTime = futureTime
        )

        assertTrue(applyBookProgressFromWebdav(existing, incoming))
        val laterServerTime = nextBookProgressTime(
            "future-webdav-book",
            persistedTime = existing.durChapterTime
        )
        applyBookProgress(
            existing,
            BookChapter(index = 1, title = "backread"),
            0,
            BOOK_PROGRESS_TEXT_OFFSET,
            laterServerTime
        )

        assertTrue(laterServerTime > futureTime)
        assertEquals(1, existing.durChapterIndex)
        assertEquals(0, existing.durChapterPos)
    }

    @Test
    fun `webdav progress rejects invalid typed positions`() {
        val validIdentity = mapOf(
            "bookUrl" to "book",
            "name" to "name",
            "author" to "author",
            "durChapterTime" to 100
        )

        assertNull(
            parseWebdavBookProgress(
                io.vertx.core.json.JsonObject(
                    validIdentity + mapOf(
                        "durChapterIndex" to -1,
                        "durChapterPos" to 0,
                        "durChapterPositionType" to BOOK_PROGRESS_TEXT_OFFSET
                    )
                )
            )
        )
        assertNull(
            parseWebdavBookProgress(
                io.vertx.core.json.JsonObject(
                    validIdentity + mapOf(
                        "durChapterIndex" to 1.5,
                        "durChapterPos" to 0,
                        "durChapterPositionType" to BOOK_PROGRESS_TEXT_OFFSET
                    )
                )
            )
        )
        assertNull(
            parseWebdavBookProgress(
                io.vertx.core.json.JsonObject(
                    validIdentity + mapOf(
                        "durChapterIndex" to 0,
                        "durChapterPos" to 1_000_001,
                        "durChapterPositionType" to BOOK_PROGRESS_CHAPTER_RATIO
                    )
                )
            )
        )
        assertNull(
            parseWebdavBookProgress(
                io.vertx.core.json.JsonObject(
                    validIdentity + mapOf(
                        "durChapterIndex" to 0,
                        "durChapterPos" to 0,
                        "durChapterPositionType" to "pixels"
                    )
                )
            )
        )
    }

    @Test
    fun `webdav import rejects older progress and accepts equal or newer progress`() {
        val existing = Book(
            durChapterIndex = 4,
            durChapterPos = 500_000,
            durChapterPositionType = BOOK_PROGRESS_CHAPTER_RATIO,
            durChapterTime = 200
        )
        val older = Book(
            durChapterIndex = 1,
            durChapterPos = 10,
            durChapterPositionType = BOOK_PROGRESS_TEXT_OFFSET,
            durChapterTime = 199
        )

        assertFalse(applyBookProgressFromWebdav(existing, older))
        assertEquals(4, existing.durChapterIndex)
        assertEquals(500_000, existing.durChapterPos)

        val newer = Book(
            durChapterIndex = 2,
            durChapterTitle = "chapter 2",
            durChapterPos = 95,
            durChapterPositionType = BOOK_PROGRESS_AUDIO_SECONDS,
            durChapterTime = 200
        )
        assertTrue(applyBookProgressFromWebdav(existing, newer))
        assertEquals(2, existing.durChapterIndex)
        assertEquals("chapter 2", existing.durChapterTitle)
        assertEquals(95, existing.durChapterPos)
        assertEquals(BOOK_PROGRESS_AUDIO_SECONDS, existing.durChapterPositionType)
    }

    @Test
    fun `bookshelf backup cannot replace newer live progress`() {
        val current = Book(
            durChapterIndex = 7,
            durChapterTitle = "current",
            durChapterPos = 700,
            durChapterPositionType = BOOK_PROGRESS_TEXT_OFFSET,
            durChapterTime = 300
        )
        val olderBackup = Book(
            durChapterIndex = 2,
            durChapterTitle = "backup",
            durChapterPos = 200,
            durChapterPositionType = BOOK_PROGRESS_TEXT_OFFSET,
            durChapterTime = 200
        )

        assertTrue(preserveNewerBookProgress(current, olderBackup))
        assertEquals(7, olderBackup.durChapterIndex)
        assertEquals("current", olderBackup.durChapterTitle)
        assertEquals(700, olderBackup.durChapterPos)
        assertEquals(300, olderBackup.durChapterTime)

        val newerBackup = Book(durChapterTime = 400)
        assertFalse(preserveNewerBookProgress(current, newerBackup))
        assertEquals(400, newerBackup.durChapterTime)
    }

    @Test
    fun `exact book url wins over an earlier same-name shelf entry`() {
        val shelfBooks = listOf(
            Book(name = "same", author = "author", bookUrl = "source-a"),
            Book(name = "same", author = "author", bookUrl = "source-b")
        )

        assertEquals(
            1,
            findShelfBookIndex(
                shelfBooks,
                Book(name = "same", author = "author", bookUrl = "source-b"),
                true
            )
        )
        assertEquals(
            -1,
            findShelfBookIndex(
                shelfBooks,
                Book(name = "same", author = "author", bookUrl = "missing"),
                false
            )
        )
        assertEquals(
            0,
            findShelfBookIndex(
                shelfBooks,
                Book(name = "same", author = "author"),
                true
            )
        )
    }
}
