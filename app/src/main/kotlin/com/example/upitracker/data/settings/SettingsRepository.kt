package com.example.upitracker.data.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var dailySummaryEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_SUMMARY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_SUMMARY_ENABLED, value).apply()

    companion object {
        private const val PREFS_NAME = "upi_tracker_settings"
        private const val KEY_DAILY_SUMMARY_ENABLED = "daily_summary_enabled"
    }
}
