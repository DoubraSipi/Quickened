package com.quickened.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity

// Registers for enter-transitions. Listener receives mapped activity names.
// walking | stationary | driving | workout | unknown
object ActivityDetector {
    const val ACTION = "com.quickened.ACTIVITY_TRANSITION"

    fun start(context: Context, onDetected: (String) -> Unit) {
        ActivityReceiver.onDetected = onDetected
        val intent = Intent(context, ActivityReceiver::class.java).setAction(ACTION)
        val pi = PendingIntent.getBroadcast(
            context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val request = com.google.android.gms.location.ActivityTransitionRequest(
            listOf(
                DetectedActivity.WALKING, DetectedActivity.RUNNING, DetectedActivity.ON_FOOT,
                DetectedActivity.STILL, DetectedActivity.IN_VEHICLE, DetectedActivity.ON_BICYCLE
            ).map { ActivityTransition.Builder().setActivityType(it)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build() }
        )
        try {
            ActivityRecognition.getClient(context).requestActivityTransitionUpdates(request, pi)
        } catch (e: SecurityException) {
            // permission missing — manual toggle remains
        }
    }

    fun stop(context: Context) {
        val intent = Intent(context, ActivityReceiver::class.java).setAction(ACTION)
        val pi = PendingIntent.getBroadcast(
            context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            ActivityRecognition.getClient(context).removeActivityTransitionUpdates(pi)
        } catch (e: SecurityException) {
            // ignore
        }
    }

    fun mapType(type: Int): String = when (type) {
        DetectedActivity.WALKING, DetectedActivity.ON_FOOT -> "walking"
        DetectedActivity.RUNNING, DetectedActivity.ON_BICYCLE -> "workout"
        DetectedActivity.IN_VEHICLE -> "driving"
        DetectedActivity.STILL -> "stationary"
        else -> "unknown"
    }

    fun speechRate(activity: String): Float = when (activity) {
        "driving" -> 1.05f
        "stationary" -> 0.92f
        else -> 1.0f
    }
}
