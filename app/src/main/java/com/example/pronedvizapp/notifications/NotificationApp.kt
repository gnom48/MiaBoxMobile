package com.example.pronedvizapp.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.pronedvizapp.InitialActivity
import com.example.pronedvizapp.R
import java.time.LocalDateTime

class NotificationApp : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        val goToPendingIntent: PendingIntent = PendingIntent.getActivity(context, R.id.GOTO_INTENT_ID, Intent(context, InitialActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, context.getString(R.string.IMPORTANT_CHANNEL_NAME))
            .setSmallIcon(R.drawable.work_icon)
            .setContentTitle(intent.getStringExtra("TITLE"))
            .setContentText(intent.getStringExtra("CONTENT"))
            .addAction(NotificationCompat.Action(android.R.drawable.btn_default, "Посмотреть", goToPendingIntent))
            .setContentIntent(goToPendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

class NotificationBroadcast : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        val goToPendingIntent: PendingIntent = PendingIntent.getActivity(context, R.id.GOTO_INTENT_ID, Intent(context, InitialActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, context.getString(R.string.NOT_IMPORTANT_CHANNEL_NAME))
            .setSmallIcon(R.drawable.work_icon)
            .setContentTitle(intent.getStringExtra("TITLE"))
            .setContentText(intent.getStringExtra("CONTENT"))
            .addAction(NotificationCompat.Action(android.R.drawable.btn_default, "Посмотреть", goToPendingIntent))
            .setContentIntent(goToPendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

class MorningNotificationBroadcast : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        if (LocalDateTime.now().dayOfWeek.value == 6 || LocalDateTime.now().dayOfWeek.value == 7) {
            //return
        }
        if (LocalDateTime.now().hour > 19 || LocalDateTime.now().hour < 9) {
            //return
        }

        val goToPendingIntent: PendingIntent = PendingIntent.getActivity(context, R.id.GOTO_INTENT_ID, Intent(context, InitialActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, context.getString(R.string.NOT_IMPORTANT_CHANNEL_NAME))
            .setSmallIcon(R.drawable.work_icon)
            .setContentTitle(intent.getStringExtra("TITLE"))
            .setContentText(intent.getStringExtra("CONTENT"))
            .addAction(NotificationCompat.Action(android.R.drawable.btn_default, "Посмотреть", goToPendingIntent))
            .setContentIntent(goToPendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}


