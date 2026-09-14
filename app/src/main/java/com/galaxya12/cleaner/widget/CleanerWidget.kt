package com.galaxya12.cleaner.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.galaxya12.cleaner.MainActivity
import com.galaxya12.cleaner.cleaner.StorageAnalyzer
import com.galaxya12.cleaner.model.StorageInfo

class CleanerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val storageAnalyzer = StorageAnalyzer(context)
        val storageInfo = storageAnalyzer.queryStorageInfo()
        val junkBytes = getCachedJunkBytes(context)

        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId, junkBytes, storageInfo)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_SCAN -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("action", "SCAN_STORAGE")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(launchIntent)
            }
            ACTION_WIDGET_CLEAN -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("action", "CLEAN_JUNK")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(launchIntent)
            }
            ACTION_UPDATE_DATA -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, CleanerWidget::class.java)
                )
                val storageAnalyzer = StorageAnalyzer(context)
                val storageInfo = storageAnalyzer.queryStorageInfo()
                val junkBytes = getCachedJunkBytes(context)
                for (id in ids) {
                    updateWidget(context, appWidgetManager, id, junkBytes, storageInfo)
                }
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_SCAN = "com.galaxya12.cleaner.WIDGET_SCAN"
        const val ACTION_WIDGET_CLEAN = "com.galaxya12.cleaner.WIDGET_CLEAN"
        const val ACTION_UPDATE_DATA = "com.galaxya12.cleaner.WIDGET_UPDATE_DATA"
        private const val PREFS_NAME = "cleaner_widget_prefs"
        private const val KEY_JUNK_BYTES = "key_junk_bytes"

        fun setCachedJunkBytes(context: Context, bytes: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_JUNK_BYTES, bytes).apply()
        }

        fun getCachedJunkBytes(context: Context): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong(KEY_JUNK_BYTES, 0L)
        }

        fun notifyDataChanged(context: Context) {
            val intent = Intent(context, CleanerWidget::class.java).apply {
                action = ACTION_UPDATE_DATA
            }
            context.sendBroadcast(intent)
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            junkBytes: Long,
            storageInfo: StorageInfo
        ) {
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)

            val views = if (minWidth >= 200) {
                // Medium Widget
                RemoteViews(context.packageName, R.layout.widget_cleaner_medium).apply {
                    val ratioText = "${storageInfo.formattedUsed} / ${storageInfo.formattedTotal}"
                    setTextViewText(R.id.widget_storage_ratio, "Storage: $ratioText")
                    setTextViewText(R.id.widget_cleanable_text, StorageInfo.formatBytes(junkBytes))

                    val scanPending = PendingIntent.getBroadcast(
                        context,
                        101,
                        Intent(context, CleanerWidget::class.java).apply { action = ACTION_WIDGET_SCAN },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setOnClickPendingIntent(R.id.widget_btn_scan, scanPending)

                    val cleanPending = PendingIntent.getBroadcast(
                        context,
                        102,
                        Intent(context, CleanerWidget::class.java).apply { action = ACTION_WIDGET_CLEAN },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setOnClickPendingIntent(R.id.widget_btn_clean_med, cleanPending)
                }
            } else {
                // Small Widget
                RemoteViews(context.packageName, R.layout.widget_cleaner_small).apply {
                    setTextViewText(R.id.widget_junk_text, StorageInfo.formatBytes(junkBytes))

                    val cleanPending = PendingIntent.getBroadcast(
                        context,
                        103,
                        Intent(context, CleanerWidget::class.java).apply { action = ACTION_WIDGET_CLEAN },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setOnClickPendingIntent(R.id.widget_btn_clean, cleanPending)
                }
            }

            // Clicking root area opens main application
            val appPending = PendingIntent.getActivity(
                context,
                100,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(
                if (minWidth >= 200) R.id.widget_medium_root else R.id.widget_small_root,
                appPending
            )

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
