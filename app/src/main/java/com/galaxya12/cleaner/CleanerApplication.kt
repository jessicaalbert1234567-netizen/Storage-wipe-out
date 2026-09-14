package com.galaxya12.cleaner

import android.app.Application

class CleanerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: CleanerApplication
            private set
    }
}
