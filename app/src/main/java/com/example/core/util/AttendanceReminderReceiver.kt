package com.example.core.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class AttendanceReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS_NOTIFICATION = "com.example.ACTION_DISMISS_NOTIFICATION"
        const val EXTRA_NOTIFICATION_ID_TO_CANCEL = "extra_notification_id_to_cancel"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == "android.intent.action.MY_PACKAGE_REPLACED") {
            AttendanceReminderHelper.scheduleDailyReminders(context)
            return
        }

        if (intent?.action == ACTION_DISMISS_NOTIFICATION) {
            val idToCancel = intent.getIntExtra(EXTRA_NOTIFICATION_ID_TO_CANCEL, -1)
            if (idToCancel != -1) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(idToCancel)
            }
            return
        }

        val title = intent?.getStringExtra(AttendanceReminderHelper.EXTRA_TITLE)
            ?: "Attendance Reminder"
        val message = intent?.getStringExtra(AttendanceReminderHelper.EXTRA_MESSAGE)
            ?: "Don't forget to mark today's labor attendance!"
        val notificationId = intent?.getIntExtra(
            AttendanceReminderHelper.EXTRA_NOTIFICATION_ID,
            AttendanceReminderHelper.REQUEST_CODE_MORNING
        ) ?: AttendanceReminderHelper.REQUEST_CODE_MORNING

        AttendanceReminderHelper.createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Mark Present (launches MainActivity)
        val markPresentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val markPresentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 100,
            markPresentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Dismiss (dismisses the notification cleanly)
        val dismissIntent = Intent(context, AttendanceReminderReceiver::class.java).apply {
            action = ACTION_DISMISS_NOTIFICATION
            putExtra(EXTRA_NOTIFICATION_ID_TO_CANCEL, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 200,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val brandColor = Color.parseColor("#1D61D2")

        val notification = NotificationCompat.Builder(context, AttendanceReminderHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setColor(brandColor)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setBigContentTitle(title)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLights(brandColor, 1000, 1000)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Mark Present", markPresentPendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)

        // Reschedule daily reminders for upcoming days
        AttendanceReminderHelper.scheduleDailyReminders(context)
    }
}
