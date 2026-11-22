package com.example.upitracker.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.upitracker.MainActivity
import com.example.upitracker.R
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Budget
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.data.repository.BudgetRepository
import com.example.upitracker.data.repository.TransactionRepository
import com.example.upitracker.domain.budget.BudgetLogic
import com.example.upitracker.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId

class BudgetAlertChecker(
    private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    companion object {
        const val CHANNEL_ID = "budget_alerts"
        const val CHANNEL_NAME = "Budget alerts"
        private const val PREFS_NAME = "budget_alerts"
        private const val KEY_NOTIFIED_MARKERS = "notified_markers"

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

    private val mutex = Mutex()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Checks budgets against current month transactions and triggers alerts if thresholds are crossed.
     */
    suspend fun check() = mutex.withLock {
        val currentMonth = YearMonth.now(clock.withZone(zoneId))
        val currentMonthPrefix = "$currentMonth|"

        val budgets = budgetRepository.getAll().associate { it.budgetKey to it.limitPaise }
        if (budgets.isEmpty()) return@withLock

        val startMillis = DateTimeUtils.monthStartMillis(currentMonth, zoneId)
        val endMillis = DateTimeUtils.monthEndExclusiveMillis(currentMonth, zoneId)
        val transactions = transactionRepository.getTransactionsBetween(startMillis, endMillis).first()

        val spentByKey = mutableMapOf<String, Long>()
        var totalSpent = 0L
        for (tx in transactions) {
            if (tx.type == TransactionType.DEBIT) {
                totalSpent += tx.amountPaise
                val currentCatSpent = spentByKey[tx.category] ?: 0L
                spentByKey[tx.category] = currentCatSpent + tx.amountPaise
            }
        }
        spentByKey[Budget.TOTAL_BUDGET_KEY] = totalSpent

        val storedSet = prefs.getStringSet(KEY_NOTIFIED_MARKERS, emptySet()) ?: emptySet()
        // Clean up markers: keep only those belonging to current month
        val alreadyNotified = storedSet.filter { it.startsWith(currentMonthPrefix) }.toSet()

        val decision = BudgetLogic.decideAlerts(
            budgets = budgets,
            spentByKey = spentByKey,
            month = currentMonth,
            alreadyNotified = alreadyNotified
        )

        // Show notifications for items in decision.toNotify
        if (hasNotificationPermission()) {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                for (item in decision.toNotify) {
                    showNotification(notificationManager, item.budgetKey, item.threshold)
                }
            }
        }

        // Update markers in SharedPreferences, preserving already-notified markers for the current month
        val updatedSet = HashSet(storedSet.filter { !it.startsWith(currentMonthPrefix) })
        updatedSet.addAll(alreadyNotified)
        updatedSet.addAll(decision.toMark)
        prefs.edit().putStringSet(KEY_NOTIFIED_MARKERS, updatedSet).apply()
    }

    /**
     * Called when a budget is saved or edited.
     * Removes markers of that budget for the current month, recomputes, and marks all crossed
     * thresholds WITHOUT notifying.
     */
    suspend fun resetAndMark(budgetKey: String) = mutex.withLock {
        val currentMonth = YearMonth.now(clock.withZone(zoneId))
        val budgetKeyMarker = "$currentMonth|$budgetKey|"

        val storedSet = prefs.getStringSet(KEY_NOTIFIED_MARKERS, emptySet()) ?: emptySet()
        val cleanedSet = storedSet.filterNot { it.startsWith(budgetKeyMarker) }.toMutableSet()

        val budgets = budgetRepository.getAll().associate { it.budgetKey to it.limitPaise }
        val limit = budgets[budgetKey] ?: return@withLock
        if (limit <= 0L) {
            prefs.edit().putStringSet(KEY_NOTIFIED_MARKERS, cleanedSet).apply()
            return@withLock
        }

        val startMillis = DateTimeUtils.monthStartMillis(currentMonth, zoneId)
        val endMillis = DateTimeUtils.monthEndExclusiveMillis(currentMonth, zoneId)
        val transactions = transactionRepository.getTransactionsBetween(startMillis, endMillis).first()

        var spent = 0L
        for (tx in transactions) {
            if (tx.type == TransactionType.DEBIT) {
                if (budgetKey == Budget.TOTAL_BUDGET_KEY || tx.category == budgetKey) {
                    spent += tx.amountPaise
                }
            }
        }

        val spentPaise = spent
        val budgetPaise = limit
        val percent = ((spentPaise * 100) / budgetPaise).toInt()
        if (percent >= 80) cleanedSet.add("$currentMonth|$budgetKey|80")
        if (percent >= 100) cleanedSet.add("$currentMonth|$budgetKey|100")

        prefs.edit().putStringSet(KEY_NOTIFIED_MARKERS, HashSet(cleanedSet)).apply()
    }

    /**
     * Called when a budget is deleted. Removes all stored markers for [budgetKey].
     */
    suspend fun deleteMarkers(budgetKey: String) = mutex.withLock {
        val markerPart = "|$budgetKey|"
        val storedSet = prefs.getStringSet(KEY_NOTIFIED_MARKERS, emptySet()) ?: emptySet()
        val cleanedSet = storedSet.filterNot { it.contains(markerPart) }.toSet()
        prefs.edit().putStringSet(KEY_NOTIFIED_MARKERS, HashSet(cleanedSet)).apply()
    }

    private fun showNotification(
        notificationManager: NotificationManagerCompat,
        budgetKey: String,
        threshold: Int
    ) {
        val title = "Budget alert"
        val body = if (budgetKey == Budget.TOTAL_BUDGET_KEY) {
            if (threshold >= 100) {
                "Overall monthly budget is over its limit"
            } else {
                "Overall monthly budget is 80% used"
            }
        } else {
            val cat = Category.fromLabel(budgetKey)
            if (threshold >= 100) {
                "${cat.emoji} ${cat.label} budget is over its limit (100% used)"
            } else {
                "${cat.emoji} ${cat.label} budget is 80% used"
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            budgetKey.hashCode() and 0x7fffffff,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_budget)
            .setContentTitle("Budget alert")
            .setContentText("Open the app for details")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_budget)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .build()

        try {
            val notifId = budgetKey.hashCode() and 0x7fffffff
            notificationManager.notify(notifId, notification)
        } catch (_: SecurityException) {
            // Handled when permission is missing
        }
    }

    private fun hasNotificationPermission(): Boolean {
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
