package com.example.upitracker.service

import android.Manifest
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class DailySummaryReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_SUMMARY = "com.example.upitracker.action.DAILY_SUMMARY"
        const val CHANNEL_ID = "daily_summary"
        const val CHANNEL_NAME = "Daily Summary"
        private const val NOTIFICATION_ID = 2002

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val settingsRepository = SettingsRepository(context)

        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            if (settingsRepository.dailySummaryEnabled) {
                DailySummaryScheduler.scheduleNext(context)
            }
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!settingsRepository.dailySummaryEnabled) {
                    return@launch
                }

                if (!hasNotificationPermission(context)) {
                    return@launch
                }

                val notificationManager = NotificationManagerCompat.from(context)
                if (!notificationManager.areNotificationsEnabled()) {
                    return@launch
                }

                createNotificationChannel(context)

                val zoneId = ZoneId.systemDefault()
                val nowLocalDate = LocalDate.now(zoneId)
                val currentMonth = YearMonth.now(zoneId)

                val dayStartMillis = nowLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val dayEndMillis = nowLocalDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

                val monthStartMillis = DateTimeUtils.monthStartMillis(currentMonth, zoneId)
                val monthEndMillis = DateTimeUtils.monthEndExclusiveMillis(currentMonth, zoneId)

                val database = AppDatabase.getDatabase(context)
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

                postNotification(context, notificationManager, title, body)
            } catch (_: Exception) {
                // Ignore failure
            } finally {
                DailySummaryScheduler.scheduleNext(context)
                pendingResult.finish()
            }
        }
    }

    private fun postNotification(
        context: Context,
        notificationManager: NotificationManagerCompat,
        title: String,
        body: String
    ) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_budget)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
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
