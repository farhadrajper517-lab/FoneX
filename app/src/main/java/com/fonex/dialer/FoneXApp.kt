package com.fonex.dialer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class FoneXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Prefs.themeMode(this))
    }
}
