package com.galaxya12.cleaner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.galaxya12.cleaner.cleaner.DuplicateScanner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DuplicateScannerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDuplicateGroupingAndHashMatching() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testDir = tempFolder.newFolder("duplicate_test")

        val payload = "A".repeat(2048) // 2KB payload

        // Create two identical files
        val file1 = File(testDir, "photo1.jpg").apply { writeText(payload) }
        val file2 = File(testDir, "photo2.jpg").apply { writeText(payload) }

        // Create a different file with exact same size (different content)
        val file3Different = File(testDir, "photo3.jpg").apply { writeText("B".repeat(2048)) }

        // Create a file with different size
        val file4DifferentSize = File(testDir, "photo4.jpg").apply { writeText("C".repeat(1500)) }

        val scanner = DuplicateScanner(context)
        val groups = scanner.scanDuplicates(listOf(testDir))

        assertEquals("Should find exactly 1 group of duplicates", 1, groups.size)
        val group = groups.first()
        assertEquals("Group should contain exactly 2 matching duplicate items", 2, group.items.size)

        // Verify the original item is not selected by default
        val originalItem = group.items.firstOrNull { it.isOriginal }
        assertTrue("Group must designate an original copy", originalItem != null)
        assertFalse("Original file must not be selected by default", originalItem!!.isSelected)

        // Verify non-original candidate duplicate is selected for user review
        val duplicateItem = group.items.firstOrNull { !it.isOriginal }
        assertTrue("Duplicate copy must be detected", duplicateItem != null)
        assertTrue("Duplicate candidate should be selected", duplicateItem!!.isSelected)
    }
}
