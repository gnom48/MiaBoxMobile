package com.example.pronedvizapp.bisness

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databases.DbViewModel
import com.example.pronedvizapp.databases.LocalDb
import com.example.pronedvizapp.databases.models.ActiveWorkOrm
import com.example.pronedvizapp.notifications.NotificationApp
import com.example.pronedvizapp.notifications.NotificationBroadcast
import com.example.pronedvizapp.requests.ServerApiTasks
import com.example.pronedvizapp.requests.models.Task
import com.example.pronedvizapp.requests.models.WorkTasksTypes
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.UUID

@Suppress("Since15")
abstract class Work(val context: Context, val workType: WorkTasksTypes) {
    lateinit var workDuration: Duration
    lateinit var workStartTime: LocalDateTime
    var notificationId: String? = null

    fun type() = this::class

    override fun toString(): String = "Duration = '$workDuration' | IsControlled = '$notificationId' | type = '${type()}'"

    fun saveToDb() {
        val dbContext = LocalDb.getDb(context)
        val newWork = ActiveWorkOrm(
            null,
            MainStatic.currentUser?.id!!.toLong(),
            "Работа",
            workStartTime.toEpochSecond(ZoneOffset.UTC),
            "Описание",
            workDuration.seconds,
            notificationId
        )
        //var tViewModel = ViewModelProvider(context).get(DbViewModel::class.java)
//        try {
//            tViewModel.insertActiveWork(newWork)
//        } catch (e: Exception) {
//            Toast.makeText(context.requireContext(), "Ошибка записи в бд", Toast.LENGTH_SHORT).show()
//        }
    }

    @SuppressLint("NewApi", "ScheduleExactAlarm")
    open fun start(_workDuration: Duration, _isControlled: Boolean) {
        workStartTime = LocalDateTime.now()
        workDuration = _workDuration

        val notifId = System.currentTimeMillis().toInt()

        if (_isControlled) {
            val calendar = Calendar.getInstance()
            calendar.set(workStartTime.year, workStartTime.monthValue-1, workStartTime.dayOfMonth, workStartTime.hour, workStartTime.minute, workStartTime.second)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, NotificationApp::class.java).let {
                    intent ->
                intent.putExtra("TITLE", "Уведомление")
                intent.putExtra("CONTENT", "Как прошла работа над ${this.workType.description}?")
                PendingIntent.getBroadcast(context, notifId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }

            val time = workDuration.toMillis() + calendar.timeInMillis
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarmIntent)
        }

        //saveToDb(context)
        val newTask = Task(0, workType.description, workStartTime.toEpochSecond(ZoneOffset.UTC), "Работа", workDuration.toSeconds().toInt(), MainStatic.currentUser!!.id, notifId)

        val retrofit = Retrofit.Builder()
            .baseUrl(context.getString(R.string.server_ip_address))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val taskApi = retrofit.create(ServerApiTasks::class.java)

        val req = taskApi.addTask(newTask, MainStatic.currentToken!!)
        var resultAddition: Int? = null
        req.enqueue(object : Callback<Int?> {
            override fun onResponse(call: Call<Int?>, response: Response<Int?>) {
                if (response.isSuccessful) {
                    resultAddition = response.body()
                    return
                }
                resultAddition = null
                Toast.makeText(context, "Ошибка ответа сервера", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(call: Call<Int?>, t: Throwable) {
                resultAddition = null
                Toast.makeText(context, "Ошибка записи", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

class Analytics(val _context: Context) : Work(_context, WorkTasksTypes.ANALYTICS) {

}


class OtherWork(val _context: Context, val _workType: WorkTasksTypes) : Work(_context, _workType) {

}

class CustomeWork(val _context: Context, val _workType: WorkTasksTypes) : Work(_context, _workType) {
    var desc: String = ""

    @SuppressLint("NewApi", "ScheduleExactAlarm")
    override fun start(_workDuration: Duration, _isControlled: Boolean) {
        workStartTime = LocalDateTime.now()
        workDuration = _workDuration

        val notifId = System.currentTimeMillis().toInt()

        if (_isControlled) {
            val calendar = Calendar.getInstance()
            calendar.set(workStartTime.year, workStartTime.monthValue-1, workStartTime.dayOfMonth, workStartTime.hour, workStartTime.minute, workStartTime.second)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, NotificationApp::class.java).let {
                    intent ->
                intent.putExtra("TITLE", "Уведомление")
                intent.putExtra("CONTENT", "Как прошла работа над ${this.workType.description}?")
                PendingIntent.getBroadcast(context, notifId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }

            val time = workDuration.toMillis() + calendar.timeInMillis
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarmIntent)
        }

        //saveToDb(context)
        val newTask = Task(0, workType.description, workStartTime.toEpochSecond(ZoneOffset.UTC), this.desc, workDuration.toSeconds().toInt(), MainStatic.currentUser!!.id, notifId)

        val retrofit = Retrofit.Builder()
            .baseUrl(context.getString(R.string.server_ip_address))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val taskApi = retrofit.create(ServerApiTasks::class.java)

        val req = taskApi.addTask(newTask, MainStatic.currentToken!!)
        var resultAddition: Int? = null
        req.enqueue(object : Callback<Int?> {
            override fun onResponse(call: Call<Int?>, response: Response<Int?>) {
                if (response.isSuccessful) {
                    resultAddition = response.body()
                    return
                }
                resultAddition = null
                Toast.makeText(context, "Ошибка ответа сервера", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(call: Call<Int?>, t: Throwable) {
                resultAddition = null
                Toast.makeText(context, "Ошибка записи", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
