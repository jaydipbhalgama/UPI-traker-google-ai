package com.example.upitracker

import android.app.Application
import com.example.upitracker.data.settings.SettingsRepository
import com.example.upitracker.di.AppContainer
import com.example.upitracker.di.DefaultAppContainer
import com.example.upitracker.service.BudgetAlertChecker
import com.example.upitracker.service.DailySummaryReceiver
import com.example.upitracker.service.DailySummaryScheduler

class UpiTrackerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        BudgetAlertChecker.createNotificationChannel(this)
        DailySummaryReceiver.createNotificationChannel(this)
        container = DefaultAppContainer(this)
        if (SettingsRepository(this).dailySummaryEnabled) {
            DailySummaryScheduler.scheduleNext(this)
        }
    }
}
