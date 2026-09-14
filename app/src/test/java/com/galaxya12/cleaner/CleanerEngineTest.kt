package com.galaxya12.cleaner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.galaxya12.cleaner.cleaner.CleanerEngine
import com.galaxya12.cleaner.model.CleanableCategory
import com.galaxya12.cleaner.model.CleanableItem
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CleanerEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testExecuteCleanDeletesOnlySelectedFiles() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = CleanerEngine(context)

        val fileToClean = tempFolder.newFile("garbage.tmp").apply {
            writeText("Hello junk 1234567890")
        }
        val fileToKeep = tempFolder.newFile("keep_me.tmp").apply {
            writeText("Do not touch")
        }

        val itemToClean = CleanableItem(
            id = UUID.randomUUID().toString(),
            name = fileToClean.name,
            path = fileToClean.absolutePath,
            sizeBytes = fileToClean.length(),
            lastModified = fileToClean.lastModified(),
            category = CleanableCategory.JUNK_FILES,
            canDeleteDirectly = true,
            isSelected = true
        )

        val itemToKeep = CleanableItem(
            id = UUID.randomUUID().toString(),
            name = fileToKeep.name,
            path = fileToKeep.absolutePath,
            sizeBytes = fileToKeep.length(),
            lastModified = fileToKeep.lastModified(),
            category = CleanableCategory.JUNK_FILES,
            canDeleteDirectly = true,
            isSelected = false // Not selected
        )

        val expectedFreedBytes = fileToClean.length()
        val result = engine.executeClean(listOf(itemToClean, itemToKeep))

        assertEquals("Should report 1 deleted file", 1, result.deletedCount)
        assertEquals("Should report 0 failed files", 0, result.failedCount)
        assertEquals("Freed bytes must match deleted file length", expectedFreedBytes, result.freedBytes)

        assertFalse("Cleaned file must no longer exist on filesystem", fileToClean.exists())
        assertTrue("Unselected file must still exist untouched", fileToKeep.exists())
    }

    @Test
    fun testNeverFalselyReportsDeletionForMissingOrProtectedFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = CleanerEngine(context)

        val missingItem = CleanableItem(
            id = UUID.randomUUID().toString(),
            name = "phantom.tmp",
            path = "/non/existent/path/phantom.tmp",
            sizeBytes = 1024L,
            lastModified = 0L,
            category = CleanableCategory.JUNK_FILES,
            canDeleteDirectly = true,
            isSelected = true
        )

        val result = engine.executeClean(listOf(missingItem))

        assertEquals("Missing file should count as failed", 1, result.failedCount)
        assertEquals("Deleted count must be 0", 0, result.deletedCount)
        assertEquals("Freed bytes must be 0", 0L, result.freedBytes)
    }
}
