package com.ojasvi.chatz.receiver

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ojasvi.chatz.R
import com.ojasvi.chatz.workers.MessageWorker
import java.util.concurrent.TimeUnit

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("package") ?: return
        val remoteInputKey = intent.getStringExtra("remoteInputKey") ?: return
        val pendingIntentId = intent.getStringExtra("pendingIntentId") ?: return

        val replyText = RemoteInput.getResultsFromIntent(intent)?.getCharSequence("user_reply")?.toString()
        if (replyText.isNullOrEmpty()) return

        Log.d("ReplyReceiver", "User replied: $replyText")

        scheduleMessage(context, replyText, packageName, remoteInputKey, pendingIntentId)

        updateNotification(context, replyText) // ✅ Update the notification to stop loading state
    }

    private fun updateNotification(context: Context, replyText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val updatedNotification = NotificationCompat.Builder(context, "chatz_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText("Reply sent: $replyText") // ✅ Notify user that reply is sent
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, updatedNotification) // ✅ Update existing notification
    }

    private fun scheduleMessage(
        context: Context,
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
                    "pendingIntentId" to pendingIntentId
                )
            )
            .setInitialDelay(5, TimeUnit.SECONDS) // Adjust delay if needed
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d("ChatZ", "Reply scheduled: $message")
    }
}
