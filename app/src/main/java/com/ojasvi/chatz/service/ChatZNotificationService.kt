package com.ojasvi.chatz.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.ojasvi.chatz.receiver.ReplyReceiver
import com.ojasvi.chatz.utils.PendingIntentStore
import java.util.UUID

class ChatZNotificationService : NotificationListenerService() {
    private val replyChannelId = "chatz_reply_channel"
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName != "org.telegram.messenger") return

        val extras = sbn.notification.extras
        val replyAction = sbn.notification.actions?.find { it.remoteInputs != null } ?: return

        val receivedMessage = extras.getCharSequence("android.text")?.toString() ?: return
        val pendingIntent = replyAction.actionIntent
        val remoteInputKey = replyAction.remoteInputs?.first()?.resultKey ?: return

        val uniqueId = UUID.randomUUID().toString()
        PendingIntentStore.save(uniqueId, pendingIntent)

        // Cancel the original notification
        cancelNotification(sbn.key)

        // Show custom notification for user input
        showReplyNotification(receivedMessage, packageName, remoteInputKey, uniqueId)
    }

    private fun showReplyNotification(
        receivedMessage: String,
        packageName: String,
        remoteInputKey: String,
        pendingIntentId: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(replyChannelId, "ChatZ Replies", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val remoteInput = RemoteInput.Builder("user_reply").setLabel("Type your reply").build()

        val replyIntent = Intent(this, ReplyReceiver::class.java).apply {
            action = "com.ojasvi.chatz.REPLY_ACTION"
            putExtra("package", packageName)
            putExtra("remoteInputKey", remoteInputKey)
            putExtra("pendingIntentId", pendingIntentId)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            this, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send, "Reply", replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val notification = NotificationCompat.Builder(this, replyChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New Message")
            .setContentText("Message: $receivedMessage")
            .addAction(replyAction)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
