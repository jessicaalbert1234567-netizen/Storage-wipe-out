package com.galaxya12.cleaner.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object SafManager {

    private const val PREFS_NAME = "cleaner_saf_prefs"
    private const val KEY_SAF_URI = "key_persisted_saf_uri"
    private const val KEY_SAF_NAME = "key_persisted_saf_name"

    fun getPersistedUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriStr = prefs.getString(KEY_SAF_URI, null) ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            val persistedList = context.contentResolver.persistedUriPermissions
            val isStillValid = persistedList.any { it.uri == uri && it.isReadPermission }
            if (isStillValid) uri else null
        } catch (_: Exception) {
            null
        }
    }

    fun getPersistedFolderName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SAF_NAME, null)
    }

    fun hasSafAccess(context: Context): Boolean {
        return getPersistedUri(context) != null
    }

    fun savePersistedUri(context: Context, uri: Uri) {
        try {
            val takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) and
                    (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)

            val docFile = DocumentFile.fromTreeUri(context, uri)
            val folderName = docFile?.name ?: uri.lastPathSegment ?: "Selected Folder"

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_SAF_URI, uri.toString())
                .putString(KEY_SAF_NAME, folderName)
                .apply()
        } catch (e: Exception) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_SAF_URI, uri.toString())
                .putString(KEY_SAF_NAME, "Selected Folder")
                .apply()
        }
    }

    fun removeSafAccess(context: Context) {
        val uri = getPersistedUri(context)
        if (uri != null) {
            try {
                val releaseFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.releasePersistableUriPermission(uri, releaseFlags)
            } catch (_: Exception) {
            }
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SAF_URI).remove(KEY_SAF_NAME).apply()
    }
}
