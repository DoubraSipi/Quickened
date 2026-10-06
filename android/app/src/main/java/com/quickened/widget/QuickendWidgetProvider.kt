package com.quickened.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.quickened.MainActivity
import com.quickened.R

class QuickendWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val intent = Intent(context, MainActivity::class.java)
                .setAction(ACTION_BEGIN)
                .putExtra(EXTRA_BEGIN, true)
            val pi = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val views = RemoteViews(context.packageName, R.layout.widget_begin)
            views.setOnClickPendingIntent(R.id.btn_begin, pi)
            manager.updateAppWidget(id, views)
        }
    }

    companion object {
        const val ACTION_BEGIN = "com.quickened.BEGIN_MOMENT"
        const val EXTRA_BEGIN = "begin_moment"
    }
}
