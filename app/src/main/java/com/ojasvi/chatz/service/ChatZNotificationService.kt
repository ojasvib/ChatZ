package com.ojasvi.chatz.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.*
import android.util.Log
import com.chatz.worker.MessageWorker
import com.ojasvi.chatz.utils.PendingIntentStore
import java.util.concurrent.TimeUnit
import java.util.UUID

class ChatZNotificationService : NotificationListenerService() {
    val tag = "ChatZNotificationService"
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName != "com.whatsapp" && packageName != "org.telegram.messenger") return

        val extras = sbn.notification.extras
        val replyAction = sbn.notification.actions?.find { it.remoteInputs != null } ?: return

        val replyText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (replyText.isNullOrEmpty()) return

        val pendingIntent = replyAction.actionIntent
        val remoteInputKey = replyAction.remoteInputs?.first()?.resultKey ?: return

        val uniqueId = UUID.randomUUID().toString()
        PendingIntentStore.save(uniqueId, pendingIntent) // 🔹 Store the PendingIntent
        Log.d(tag, "Message received: $replyText")
        scheduleMessage(replyText, packageName, remoteInputKey, uniqueId)
        cancelNotification(sbn.key) // Prevents message from being sent instantly
    }

    private fun scheduleMessage(
        message: String,
        packageName: String,
        remoteInputKey: String,
        pendingIntentId: String
    ) {
        val workRequest = OneTimeWorkRequestBuilder<MessageWorker>()
            .setInputData(
                workDataOf(
                    "message" to message,
                    "package" to packageName,
                    "remoteInputKey" to remoteInputKey,
                    "pendingIntentId" to pendingIntentId // 🔹 Pass the unique ID
                )
            )
            .setInitialDelay(5, TimeUnit.MINUTES) // Adjust delay as needed
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
        Log.d("ChatZ", "Message scheduled: $message")
    }
}
