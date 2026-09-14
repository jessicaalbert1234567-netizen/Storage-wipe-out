package com.galaxya12.cleaner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.galaxya12.cleaner.cleaner.StorageAnalyzer
import com.galaxya12.cleaner.model.StorageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StorageAnalyzerTest {

    @Test
    fun testStorageInfoFormatting() {
        assertEquals("0 B", StorageInfo.formatBytes(0L))
        assertEquals("512 B", StorageInfo.formatBytes(512L))
        assertEquals("1.00 KB", StorageInfo.formatBytes(1024L))
        assertEquals("1.50 MB", StorageInfo.formatBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("1.24 GB", StorageInfo.formatBytes((1.24 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testStorageAnalyzerQuery() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val analyzer = StorageAnalyzer(context)
        val info = analyzer.queryStorageInfo()

        assertTrue("Total storage must be positive", info.totalBytes > 0L)
        assertTrue("Used storage must be positive or zero", info.usedBytes >= 0L)
        assertTrue("Percentage must be within 0..100", info.usedPercentage in 0..100)
    }
}
