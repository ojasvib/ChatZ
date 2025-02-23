package com.ojasvi.chatz.workers

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log
import com.ojasvi.chatz.utils.PendingIntentStore

class MessageWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val message = inputData.getString("message") ?: return Result.failure()
        val remoteInputKey = inputData.getString("remoteInputKey") ?: return Result.failure()
        val pendingIntentId = inputData.getString("pendingIntentId") ?: return Result.failure()

        val pendingIntent = PendingIntentStore.retrieve(pendingIntentId)
        if (pendingIntent == null) {
            Log.e("ChatZ", "PendingIntent not found!")
            return Result.failure()
        }

        sendReply(message, remoteInputKey, pendingIntent)
        return Result.success()
    }

    private fun sendReply(message: String, remoteInputKey: String, pendingIntent: PendingIntent) {
        val remoteInput = RemoteInput.Builder(remoteInputKey).build()
        val intent = Intent().apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        val inputData = Bundle().apply { putCharSequence(remoteInputKey, message) }
        RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, inputData)

        try {
            pendingIntent.send(applicationContext, 0, intent)
            Log.d("ChatZ", "Sent scheduled message: $message")
        } catch (e: Exception) {
            Log.e("ChatZ", "Failed to send message: ${e.message}")
        }
    }
}
