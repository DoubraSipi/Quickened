package com.quickened.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.quickened.MainActivity

class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != PrayerScheduler.ACTION) return
        val title = intent.getStringExtra(PrayerScheduler.EXTRA_TITLE) ?: "Prayer watch"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("prayer", "Prayer watches", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val note = NotificationCompat.Builder(context, "prayer")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Prayer watch: $title")
            .setContentText("Pause a moment — pray now.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(title.hashCode(), note)
    }
}
