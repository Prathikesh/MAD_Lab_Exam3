package com.example.fitsync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val task = intent.getStringExtra("task") ?: "No Task"

        vibrateDevice(context)

        showReminderNotification(context, task)
    }

    private fun showReminderNotification(context: Context, task: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannelId = "reminder_channel"

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Task Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Task Reminder"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 1000, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.drawable.reminder)
            .setContentTitle("Reminder")
            .setContentText(task)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 1000, 500))
            .setAutoCancel(true)

        notificationManager.notify(1, notificationBuilder.build())
    }

    private fun vibrateDevice(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 500, 1000, 500), -1)
            )
        } else {
            vibrator.vibrate(longArrayOf(0, 500, 1000, 500), -1)
        }
    }
}
