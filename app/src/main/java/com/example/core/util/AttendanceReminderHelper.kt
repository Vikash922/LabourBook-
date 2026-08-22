package com.example.core.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import java.util.Calendar

object AttendanceReminderHelper {

    private const val TAG = "AttendanceReminderHelper"

    const val CHANNEL_ID = "attendance_reminder_channel"
    const val CHANNEL_NAME = "Attendance Reminders"

    const val EXTRA_TITLE = "extra_reminder_title"
    const val EXTRA_MESSAGE = "extra_reminder_message"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    const val REQUEST_CODE_MORNING = 1
    const val REQUEST_CODE_AFTERNOON = 2
    const val REQUEST_CODE_EVENING = 3

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Daily reminders for labor attendance and daily updates"
                    enableVibration(true)
                    enableLights(true)
                    setSound(soundUri, audioAttributes)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleDailyReminders(context: Context) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Morning: 9:00 AM
        scheduleSingleReminder(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQUEST_CODE_MORNING,
            hourOfDay = 9,
            minute = 0,
            title = "🌞 Good Morning!",
            message = "Attendance ka time hai! ⏰"
        )

        // 2. Afternoon: 2:00 PM / 14:00
        scheduleSingleReminder(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQUEST_CODE_AFTERNOON,
            hourOfDay = 14,
            minute = 0,
            title = "☀️ Time for a Break!",
            message = "Take a little break ☕"
        )

        // 3. Evening: 6:00 PM / 18:00
        scheduleSingleReminder(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQUEST_CODE_EVENING,
            hourOfDay = 18,
            minute = 0,
            title = "Evening Checkout",
            message = "Final attendance Time! ✅"
        )
    }

    private fun scheduleSingleReminder(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        hourOfDay: Int,
        minute: Int,
        title: String,
        message: String
    ) {
        val intent = Intent(context, AttendanceReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // Add a 1-minute buffer so that if the alarm fires a few milliseconds early,
            // it doesn't mistakenly schedule itself for "today" again, causing an infinite loop.
            now.add(Calendar.MINUTE, 1)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder '$title' for ${calendar.time} (RequestCode: $requestCode)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted, falling back to standard repeating alarm: ${e.message}")
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder '$title': ${e.message}", e)
        }
    }
}
