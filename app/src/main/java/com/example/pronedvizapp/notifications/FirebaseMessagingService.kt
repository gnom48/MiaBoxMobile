package com.example.pronedvizapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import com.example.pronedvizapp.InitialActivity
import com.example.pronedvizapp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val preferences: SharedPreferences by lazy { this.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.notification?.let {
            showNotification(it.title, it.body, remoteMessage.data[EVENT_KEY])
        }
    }

    private fun showNotification(title: String?, body: String?, event: String?) {
        val channelId = applicationContext.getString(R.string.default_incoming_notification_channel_id)
        val channelName = "Default Channel"
        val notificationId = 1

        val intent = Intent(this, InitialActivity::class.java)
        var pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(applicationContext.resources.getIdentifier("on_work_task_icon", "drawable", applicationContext.packageName))
            .setColor(applicationContext.resources.getColor(R.color.transparent50, applicationContext.theme))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        when(event) {
            EVENT_MORNING -> { }
            EVENT_EVENING -> {
                val editor = preferences.edit()
                editor.putInt("EVENING_MESSAGE", (System.currentTimeMillis() / 1000).toInt()).apply()
                intent.putExtra("EVENING_MESSAGE", (System.currentTimeMillis() / 1000).toInt())
                pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                notificationBuilder.setContentIntent(pendingIntent)
            }
            else -> { }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        const val EVENT_KEY = "event"
        const val EVENT_MORNING = "morning"
        const val EVENT_EVENING = "evening"
    }
}