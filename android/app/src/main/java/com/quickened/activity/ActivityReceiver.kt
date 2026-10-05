package com.quickened.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransitionResult

class ActivityReceiver : BroadcastReceiver() {
    companion object {
        var onDetected: ((String) -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ActivityDetector.ACTION) return
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val latest = result.transitionEvents.maxByOrNull { it.elapsedRealTimeNanos } ?: return
        onDetected?.invoke(ActivityDetector.mapType(latest.activityType))
    }
}
