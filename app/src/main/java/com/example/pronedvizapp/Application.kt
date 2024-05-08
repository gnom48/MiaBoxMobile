package com.example.pronedvizapp

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.pronedvizapp.notifications.NotificationBroadcast
import java.time.LocalDateTime
import java.util.Calendar

class MyApplication : Application() {

    lateinit var alarmManager: AlarmManager

    override fun onCreate() {
        super.onCreate()
        registerAppLifecycleObserver()
    }

    private fun registerAppLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleObserver {
            @SuppressLint("ScheduleExactAlarm")
            @RequiresApi(Build.VERSION_CODES.O)
            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onAppBackgrounded() {
                val calendar = Calendar.getInstance()
                val localDateTime = LocalDateTime.now()
                calendar.set(localDateTime.year, localDateTime.monthValue-1, localDateTime.dayOfMonth, localDateTime.hour+2, localDateTime.minute)

                alarmManager = this@MyApplication.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                val alarmIntent = Intent(
                    this@MyApplication.applicationContext,
                    NotificationBroadcast::class.java
                ).let { intent ->
                    intent.putExtra("TITLE", "Уведомление")
                    intent.putExtra("CONTENT", "Пора зайти в приложение и посмотреть рабочие дела")
                    PendingIntent.getBroadcast(
                        this@MyApplication.applicationContext,
                        1,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                alarmManager.cancel(alarmIntent)

                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                if ((!(dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)) && (calendar.get(
                        Calendar.HOUR_OF_DAY) in 10..18)) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
                    }
                }
            }
        )
    }
}
