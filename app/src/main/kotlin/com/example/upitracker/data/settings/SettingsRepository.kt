package com.example.upitracker.data.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var dailySummaryEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_SUMMARY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_SUMMARY_ENABLED, value).apply()

    var lastDailySummaryDate: String?
        get() = prefs.getString(KEY_LAST_DAILY_SUMMARY_DATE, null)
        set(value) = prefs.edit().putString(KEY_LAST_DAILY_SUMMARY_DATE, value).apply()

    var hasPromptedNotificationPermission: Boolean
        get() = prefs.getBoolean(KEY_PROMPTED_NOTIFICATION_PERMISSION, false)
        set(value) = prefs.edit().putBoolean(KEY_PROMPTED_NOTIFICATION_PERMISSION, value).apply()

    companion object {
        private const val PREFS_NAME = "upi_tracker_settings"
        private const val KEY_DAILY_SUMMARY_ENABLED = "daily_summary_enabled"
        private const val KEY_LAST_DAILY_SUMMARY_DATE = "last_daily_summary_date"
        private const val KEY_PROMPTED_NOTIFICATION_PERMISSION = "prompted_notification_permission"
    }
}
