package com.galaxya12.cleaner

import com.galaxya12.cleaner.cleaner.FileSafetyPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileSafetyPolicyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testProtectedDirectoriesAreRejected() {
        val protectedPaths = listOf(
            "/storage/emulated/0/Android/data/com.other.app",
            "/storage/emulated/0/Android/obb/com.game",
            "/data/data/com.example",
            "/system/bin/sh",
            "/proc/version",
            "/sys/kernel"
        )

        for (path in protectedPaths) {
            val file = File(path)
            assertTrue("Path $path must be protected", FileSafetyPolicy.isProtectedDirectory(file))
            assertFalse("Path $path must never be safe junk", FileSafetyPolicy.isSafeJunkCandidate(file))
        }
    }

    @Test
    fun testUserMediaDirectoriesAreProtected() {
        val mediaFiles = listOf(
            File("/storage/emulated/0/DCIM/Camera/IMG_001.jpg"),
            File("/storage/emulated/0/Pictures/Screenshots/screen.png"),
            File("/storage/emulated/0/Movies/family.mp4"),
            File("/storage/emulated/0/Music/song.mp3"),
            File("/storage/emulated/0/Documents/resume.pdf"),
            File("/storage/emulated/0/WhatsApp/Media/image.jpg"),
            File("/storage/emulated/0/Telegram/video.mp4")
        )

        for (file in mediaFiles) {
            assertTrue("Directory for ${file.path} must be protected media", FileSafetyPolicy.isProtectedUserMediaDirectory(file))
            assertFalse("User media ${file.name} must never be classified as junk", FileSafetyPolicy.isSafeJunkCandidate(file))
        }
    }

    @Test
    fun testUserDocumentAndMediaExtensionsNeverClassifiedAsJunk() {
        val extensions = listOf("jpg", "png", "mp4", "mp3", "pdf", "docx", "xlsx", "zip")
        for (ext in extensions) {
            val f = tempFolder.newFile("sample_file.$ext")
            f.writeText("sample user content")
            assertTrue("Extension $ext is a user file", FileSafetyPolicy.isUserDocumentOrMediaFile(f))
            assertFalse("User file $ext must never be safe junk candidate", FileSafetyPolicy.isSafeJunkCandidate(f))
        }
    }

    @Test
    fun testSafeJunkCandidatesDetected() {
        val tempDir = tempFolder.newFolder("cache")
        val tmpFile = File(tempDir, "temp_render.tmp").apply { writeText("temp data") }
        val logFile = File(tempDir, "app_crash.log").apply { writeText("log entries") }
        val bakFile = File(tempDir, "old_config.bak").apply { writeText("backup") }
        val cacheFile = File(tempDir, "network.cache").apply { writeText("cached response") }

        assertTrue(FileSafetyPolicy.isSafeJunkCandidate(tmpFile))
        assertTrue(FileSafetyPolicy.isSafeJunkCandidate(logFile))
        assertTrue(FileSafetyPolicy.isSafeJunkCandidate(bakFile))
        assertTrue(FileSafetyPolicy.isSafeJunkCandidate(cacheFile))
    }

    @Test
    fun testApkFileDetection() {
        val apk = tempFolder.newFile("release_v1.apk")
        val txt = tempFolder.newFile("notes.txt")

        assertTrue(FileSafetyPolicy.isApkFile(apk))
        assertFalse(FileSafetyPolicy.isApkFile(txt))
    }

    @Test
    fun testEmptyFileDetectionPreservesHiddenSystemFiles() {
        val safeEmpty = tempFolder.newFile("orphaned_session.tmp")
        val nomedia = tempFolder.newFile(".nomedia")

        assertTrue(FileSafetyPolicy.isSafeEmptyFile(safeEmpty))
        assertFalse("Critical system markers like .nomedia must be preserved", FileSafetyPolicy.isSafeEmptyFile(nomedia))
    }
}
