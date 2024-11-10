package com.example.fitsync

import android.app.Notification
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

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(1000)
        }

        sendNotificationWithSoundAndVibration(context)
    }

    private fun sendNotificationWithSoundAndVibration(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannelId = "timer_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val vibrationPattern = longArrayOf(0, 1000, 500, 1000)  // Vibration pattern

            val channel = NotificationChannel(
                notificationChannelId,
                "Timer Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                this.vibrationPattern = vibrationPattern
                setSound(soundUri, Notification.AUDIO_ATTRIBUTES_DEFAULT)
            }

            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.drawable.timer)
            .setContentTitle("Time's Up!")
            .setContentText("Your set time limit has been reached.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1, notificationBuilder.build())
    }
}

