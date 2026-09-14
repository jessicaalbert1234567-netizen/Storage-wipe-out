package com.galaxya12.cleaner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.galaxya12.cleaner.cleaner.CleanerEngine
import com.galaxya12.cleaner.cleaner.JunkScanner
import com.galaxya12.cleaner.util.ByteFormatter
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JunkScannerTest {

    @Test
    fun testFullScanCleanScanCycle() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Create test cache files in app's cache dir
        val file1 = File(context.cacheDir, "test_junk_1.tmp").apply {
            writeText("A".repeat(1024 * 10)) // 10 KB
        }
        val file2 = File(context.cacheDir, "test_junk_2.tmp").apply {
            writeText("B".repeat(1024 * 20)) // 20 KB
        }

        val scanner = JunkScanner(context)
        val engine = CleanerEngine(context)

        // 2. Scan
        val scanStep1 = scanner.scanJunk().last()

        // 3. Confirm bytes > 0
        assertTrue("Cleanable bytes must be > 0", scanStep1.scanResult.cleanableBytes > 0L)

        // 4. Confirm file count > 0
        assertTrue("File count must be at least 2", scanStep1.scanResult.fileCount >= 2)
        assertTrue(scanStep1.foundItems.any { it.name == "test_junk_1.tmp" })
        assertTrue(scanStep1.foundItems.any { it.name == "test_junk_2.tmp" })

        // 5. Clean
        val itemsToClean = scanStep1.foundItems.filter { it.name.startsWith("test_junk_") }
        val cleanResult = engine.executeClean(itemsToClean)
        assertEquals("Should have deleted 2 files", 2, cleanResult.deletedCount)
        assertEquals("Failed count should be 0", 0, cleanResult.failedCount)
        assertFalse("file1 should be deleted", file1.exists())
        assertFalse("file2 should be deleted", file2.exists())

        // 6. Scan again
        val scanStep2 = scanner.scanJunk().last()

        // 7. Confirm test files are gone
        val remainingTestItems = scanStep2.foundItems.filter { it.name.startsWith("test_junk_") }
        assertEquals("Test items must be 0 after clean", 0, remainingTestItems.size)
    }

    @Test
    fun testEmptyStorageReportsZeroSafely() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Ensure cache is clean of temp files
        context.cacheDir.listFiles()?.forEach { if (it.isFile && it.name.endsWith(".tmp")) it.delete() }

        val scanner = JunkScanner(context)
        val finalStep = scanner.scanJunk().last()

        assertTrue("Final step must be finished", finalStep.progress.isFinished)
        val tempFilesFound = finalStep.foundItems.filter { it.name.endsWith(".tmp") }
        assertEquals("No temporary junk should be found", 0, tempFilesFound.size)
    }

    @Test
    fun testByteFormatterRanges() {
        assertEquals("0 B", ByteFormatter.format(0L))
        assertEquals("512 B", ByteFormatter.format(512L))
        assertEquals("1 KB", ByteFormatter.format(1024L))
        assertEquals("25.4 MB", ByteFormatter.format((25.4 * 1024 * 1024).toLong()))
        assertEquals("1.24 GB", ByteFormatter.format((1.24 * 1024 * 1024 * 1024).toLong()))
        assertEquals("2.5 TB", ByteFormatter.format((2.5 * 1024 * 1024 * 1024 * 1024).toLong()))
    }
}
