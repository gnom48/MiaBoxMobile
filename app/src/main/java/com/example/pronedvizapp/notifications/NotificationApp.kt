package com.example.pronedvizapp.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.pronedvizapp.InitialActivity
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.authentication.AuthenticationActivity
import com.example.pronedvizapp.bisness.geo.GeoService
import java.time.LocalDateTime
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class NotificationApp : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        val notification = NotificationCompat.Builder(context, context.getString(R.string.IMPORTANT_CHANNEL_NAME))
            .setSmallIcon(R.drawable.work_icon)
            .setContentTitle(intent.getStringExtra("TITLE"))
            .setContentText(intent.getStringExtra("CONTENT"))
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
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}


