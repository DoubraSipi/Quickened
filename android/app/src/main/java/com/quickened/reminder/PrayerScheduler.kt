package com.quickened.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

// Per-watch prayer alarms (inexact, battery-friendly, offline).
object PrayerScheduler {
    const val ACTION = "com.quickened.PRAYER_WATCH"
    const val EXTRA_ID = "watch_id"
    const val EXTRA_TITLE = "watch_title"

    fun schedule(context: Context, id: String, title: String, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, nextTrigger(hour, minute), AlarmManager.INTERVAL_DAY,
            pending(context, id, title)
        )
    }

    fun cancel(context: Context, id: String, title: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context, id, title))
    }

    private fun nextTrigger(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun pending(context: Context, id: String, title: String): PendingIntent {
        val intent = Intent(context, PrayerReceiver::class.java)
            .setAction(ACTION)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_TITLE, title)
        return PendingIntent.getBroadcast(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
