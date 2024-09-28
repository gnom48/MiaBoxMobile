package com.example.pronedvizapp.bisness

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.notifications.NotificationApp
import com.example.pronedvizapp.requests.RequestsRepository.addNewTask
import com.example.pronedvizapp.requests.RequestsRepository.usingLocalDataToast
import com.example.pronedvizapp.requests.models.Task
import com.example.pronedvizapp.requests.models.WorkTasksTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar

@Suppress("Since15")
abstract class Work(val context: Context, public var workType: WorkTasksTypes) {
    lateinit var workDuration: Duration
    lateinit var workStartTime: LocalDateTime
    var notificationId: String? = null

    @SuppressLint("NewApi", "ScheduleExactAlarm")
    open fun start(_workDuration: Duration, _isControlled: Boolean) {
        workStartTime = LocalDateTime.now()
        workDuration = _workDuration

        val notifId = System.currentTimeMillis().toInt()

        if (_isControlled) {
            val calendar = Calendar.getInstance()
            calendar.set(
                workStartTime.year,
                workStartTime.monthValue - 1,
                workStartTime.dayOfMonth,
                workStartTime.hour,
                workStartTime.minute,
                workStartTime.second
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, NotificationApp::class.java).let { intent ->
                intent.putExtra("TITLE", "Уведомление")
                intent.putExtra("CONTENT", "Как прошла работа над ${this.workType.description}?")
                PendingIntent.getBroadcast(
                    context,
                    notifId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            val time = workDuration.toMillis() + calendar.timeInMillis
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarmIntent)
        }

        val newTask = Task(
            workType = workType.description,
            dateTime = workStartTime.toEpochSecond(ZoneOffset.UTC),
            desc = "Работа",
            durationSeconds = workDuration.seconds.toInt(),
            userId = MainStatic.currentUser.id,
            notificationId = notifId,
            isCompleted = false
        )

        CoroutineScope(Dispatchers.Main).launch {
            val additionRes = addNewTask(context, newTask, MainStatic.currentToken)
            additionRes.onSuccess { }
            additionRes.onFailure {
                Toast.makeText(context, "Ошибка записи", Toast.LENGTH_SHORT).show()
            }
            additionRes.onCached {
                usingLocalDataToast(context)
            }
        }
    }
}

class Analytics(val _context: Context) : Work(_context, WorkTasksTypes.ANALYTICS) {

}


class OtherWork(val _context: Context, var _workType: WorkTasksTypes) : Work(_context, _workType) {

}

class CustomeWork(val _context: Context, var _workType: WorkTasksTypes) : Work(_context, _workType) {
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

        val newTask = Task(
            workType = workType.description,
            dateTime = workStartTime.toEpochSecond(ZoneOffset.UTC),
            desc = this.desc,
            durationSeconds = workDuration.seconds.toInt(),
            userId = MainStatic.currentUser.id,
            notificationId = notifId,
            isCompleted = false
        )

        CoroutineScope(Dispatchers.Main).launch {
            val additionRes = addNewTask(context, newTask, MainStatic.currentToken)
            additionRes.onSuccess { }
            additionRes.onFailure {
                Toast.makeText(context, "Ошибка записи", Toast.LENGTH_SHORT).show()
            }
            additionRes.onCached {
                usingLocalDataToast(context)
            }
        }
//        val taskApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiTasks::class.java)
//        val req = taskApi.addTask(newTask, MainStatic.currentToken!!)
//        var resultAddition: Int? = null
//        req.enqueue(object : Callback<Int?> {
//            override fun onResponse(call: Call<Int?>, response: Response<Int?>) {
//                if (response.isSuccessful) {
//                    resultAddition = response.body()
//                    return
//                }
//                resultAddition = null
//                Toast.makeText(context, "Ошибка ответа сервера", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onFailure(call: Call<Int?>, t: Throwable) {
//                resultAddition = null
//                Toast.makeText(context, "Ошибка записи", Toast.LENGTH_SHORT).show()
//            }
//        })
    }

}
