package com.quickened.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.quickened.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReminderScheduler.ACTION) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("moments", "Daily moments", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val note = NotificationCompat.Builder(context, "moments")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("A quiet moment is waiting")
            .setContentText("Walk with God today — your verse is ready.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(1, note)
    }
}
