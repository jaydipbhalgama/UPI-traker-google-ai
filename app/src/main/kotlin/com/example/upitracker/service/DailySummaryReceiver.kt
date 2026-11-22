package com.example.upitracker.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.upitracker.MainActivity
import com.example.upitracker.R
import com.example.upitracker.data.local.database.AppDatabase
import com.example.upitracker.data.local.entity.Budget
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.data.settings.SettingsRepository
import com.example.upitracker.domain.summary.DailySummaryFormatter
import com.example.upitracker.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class DailySummaryReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_SUMMARY = "com.example.upitracker.action.DAILY_SUMMARY"
        const val CHANNEL_ID = "daily_summary"
        const val CHANNEL_NAME = "Daily Summary"
        private const val NOTIFICATION_ID = 2002
        private const val POST_FIRE_RESCHEDULE_BUFFER_MINUTES = 5L

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily 8:00 PM UPI spending summary"
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }

        /**
         * Queries today's and this month's spending and immediately posts the daily summary notification.
         * Returns true if the notification was dispatched, false if notification permission/settings are disabled.
         */
        suspend fun postSummaryNow(
            context: Context,
            deduplicateToday: Boolean = false
        ): Boolean = withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val settingsRepository = SettingsRepository(appContext)
            if (!hasNotificationPermission(appContext)) {
                return@withContext false
            }

            val notificationManager = NotificationManagerCompat.from(appContext)
            if (!notificationManager.areNotificationsEnabled()) {
                return@withContext false
            }

            createNotificationChannel(appContext)

            val zoneId = ZoneId.systemDefault()
            val nowLocalDate = LocalDate.now(zoneId)
            val todayKey = nowLocalDate.toString()
            if (deduplicateToday && settingsRepository.lastDailySummaryDate == todayKey) {
                return@withContext false
            }

            val currentMonth = YearMonth.now(zoneId)
            val dayStartMillis = nowLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEndMillis = nowLocalDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

            val monthStartMillis = DateTimeUtils.monthStartMillis(currentMonth, zoneId)
            val monthEndMillis = DateTimeUtils.monthEndExclusiveMillis(currentMonth, zoneId)

            val database = AppDatabase.getDatabase(appContext)
            val todaySpentPaise = database.transactionDao()
                .getTotalByTypeBetween(TransactionType.DEBIT, dayStartMillis, dayEndMillis)
                .first() ?: 0L

            val monthSpentPaise = database.transactionDao()
                .getTotalByTypeBetween(TransactionType.DEBIT, monthStartMillis, monthEndMillis)
                .first() ?: 0L

            val budgets = database.budgetDao().getAll()
            val monthlyBudget = budgets.find { it.budgetKey == Budget.TOTAL_BUDGET_KEY }?.limitPaise

            val (title, body) = DailySummaryFormatter.format(
                todaySpentPaise = todaySpentPaise,
                monthSpentPaise = monthSpentPaise,
                monthlyBudgetPaise = monthlyBudget
            )

            val posted = postNotification(appContext, notificationManager, title, body)
            if (posted && deduplicateToday) {
                settingsRepository.lastDailySummaryDate = todayKey
            }
            posted
        }

        private fun postNotification(
            context: Context,
            notificationManager: NotificationManagerCompat,
            title: String,
            body: String
        ): Boolean {
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                contentIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_budget)
                .setContentTitle(title)
                .setContentText("Open the app for today's spending summary")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_budget)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicNotification)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .build()

            return try {
                notificationManager.notify(NOTIFICATION_ID, notification)
                true
            } catch (_: SecurityException) {
                false
            }
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val settingsRepository = SettingsRepository(context)
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        ) {
            if (settingsRepository.dailySummaryEnabled) {
                DailySummaryScheduler.scheduleNext(context)
            } else {
                DailySummaryScheduler.cancel(context)
            }
            return
        }

        if (action != ACTION_DAILY_SUMMARY) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (settingsRepository.dailySummaryEnabled) {
                    postSummaryNow(context, deduplicateToday = true)
                }
            } catch (_: Exception) {
                // Ignore failure
            } finally {
                if (settingsRepository.dailySummaryEnabled) {
                    DailySummaryScheduler.scheduleNext(
                        context = context,
                        minDelayMinutes = POST_FIRE_RESCHEDULE_BUFFER_MINUTES
                    )
                } else {
                    DailySummaryScheduler.cancel(context)
                }
                pendingResult.finish()
            }
        }
    }
}
