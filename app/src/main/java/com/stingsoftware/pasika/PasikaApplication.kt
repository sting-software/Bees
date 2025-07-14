package com.stingsoftware.pasika

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class PasikaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // Apply saved language preference
        val language = sharedPrefs.getString("language_code", null)
        if (language != null) {
            val appLocale = LocaleListCompat.forLanguageTags(language)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }

        // Apply saved theme preference
        val savedThemeMode = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedThemeMode)
    }
}
