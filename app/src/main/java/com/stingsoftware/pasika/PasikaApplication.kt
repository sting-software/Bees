package com.stingsoftware.pasika

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PasikaApplication : Application() {
    // The database and repository will be provided by Hilt modules.
    // No longer need to instantiate them here.

    override fun onCreate() {
        super.onCreate()
        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedThemeMode = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedThemeMode)
    }
}