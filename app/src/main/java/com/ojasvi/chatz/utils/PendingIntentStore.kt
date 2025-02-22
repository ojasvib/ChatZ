package com.ojasvi.chatz.utils

import android.app.PendingIntent

object PendingIntentStore {
    private val pendingIntents = mutableMapOf<String, PendingIntent>()

    fun save(id: String, pendingIntent: PendingIntent) {
        pendingIntents[id] = pendingIntent
    }

    fun retrieve(id: String): PendingIntent? {
        return pendingIntents.remove(id)
    }
}
