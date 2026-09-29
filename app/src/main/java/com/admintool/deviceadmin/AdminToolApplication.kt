package com.admintool.deviceadmin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AdminToolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
