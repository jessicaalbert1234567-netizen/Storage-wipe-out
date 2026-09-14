package com.galaxya12.cleaner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.galaxya12.cleaner.cleaner.JunkScanner
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
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
    fun testJunkScannerEmitsProgressAndCompletes() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            // Create a temporary cache file in app's cache dir
            val cacheFile = File(context.cacheDir, "sample_temp.tmp").apply {
                writeText("Test cache payload")
            }

            val scanner = JunkScanner(context)
            val finalStep = scanner.scanJunk().last()

            assertTrue("Final step must be marked finished", finalStep.progress.isFinished)
            assertTrue("Found items should contain files", finalStep.foundItems.any { it.name == "sample_temp.tmp" })

            // Clean up
            cacheFile.delete()
        }
    }
}
