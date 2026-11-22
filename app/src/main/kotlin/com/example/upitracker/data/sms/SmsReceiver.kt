package com.example.upitracker.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.upitracker.UpiTrackerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered when a new SMS is received.
 *
 * It uses goAsync() and waits ~3 seconds so the message is committed to the SMS provider,
 * then triggers SmsSyncManager.syncRecent() to read and insert it with the standard smsHash.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val app = context.applicationContext as? UpiTrackerApplication ?: return
        val syncManager = app.container.smsSyncManager
        val alertChecker = app.container.budgetAlertChecker

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Wait 3 seconds for the Android OS to commit the SMS into ContentProvider
                delay(3000)
                val result = syncManager.syncRecent()
                if (result.imported > 0) {
                    alertChecker.check()
                }
            } catch (_: Exception) {
                // Keep app resilient, do not crash on receiver failures
            } finally {
                pendingResult.finish()
            }
        }
    }
}
