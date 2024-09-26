package com.example.smartarcle.data

import android.content.Context
import android.content.res.Configuration
import java.util.*

object LocaleLanguage {
    private const val LANGUAGE_KEY = "language_key"
    private const val PREFS_NAME = "settings"

    fun setLocale(context: Context, language: String) {
        persistLanguage(context, language)
        updateResources(context, language)
    }

    fun getCurrentLanguage(context: Context): String {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return preferences.getString(LANGUAGE_KEY, "en") ?: "en"
    }

    private fun persistLanguage(context: Context, language: String) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString(LANGUAGE_KEY, language).apply()
    }

    private fun updateResources(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        context.applicationContext.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}