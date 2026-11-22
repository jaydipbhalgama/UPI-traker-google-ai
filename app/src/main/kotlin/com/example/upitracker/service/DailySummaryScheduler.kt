package com.example.upitracker.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

object DailySummaryScheduler {

    private const val REQUEST_CODE = 2001
    private const val TARGET_HOUR = 20
    private const val TARGET_MINUTE = 0

    fun scheduleNext(context: Context, minDelayMinutes: Long = 0L) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = getPendingIntent(context)

        val zoneId = ZoneId.systemDefault()
        val triggerAtMillis = nextTriggerAtMillis(
            now = LocalDateTime.now(zoneId),
            zoneId = zoneId,
            minDelayMinutes = minDelayMinutes
        )

        val canUseExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canUseExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return
            } catch (_: SecurityException) {
                // Fall back to inexact allow-while-idle alarm
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    internal fun nextTriggerAtMillis(
        now: LocalDateTime,
        zoneId: ZoneId = ZoneId.systemDefault(),
        minDelayMinutes: Long = 0L
    ): Long {
        val earliestAllowed = now.plusMinutes(minDelayMinutes)
        var target = now.withHour(TARGET_HOUR).withMinute(TARGET_MINUTE).withSecond(0).withNano(0)
        if (!target.isAfter(earliestAllowed)) {
            target = target.plusDays(1)
        }
        return target.atZone(zoneId).toInstant().toEpochMilli()
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = getPendingIntent(context)
        alarmManager.cancel(pendingIntent)
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailySummaryReceiver::class.java).apply {
            action = DailySummaryReceiver.ACTION_DAILY_SUMMARY
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
