package com.example.pronedvizapp

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.example.pronedvizapp.bisness.geo.GeoService
import com.example.pronedvizapp.bisness.geo.GeoServiceBroadcast
import com.example.pronedvizapp.bisness.geo.GeoWorker
import com.example.pronedvizapp.databinding.ActivityMainBinding
import com.example.pronedvizapp.databinding.FragmentActionResultBinding
import com.example.pronedvizapp.main.CreateEditNoteFragment
import com.example.pronedvizapp.main.MainFragment
import com.example.pronedvizapp.main.NotesFragment
import com.example.pronedvizapp.main.ProfileFragment
import com.example.pronedvizapp.main.WorkFragment
import com.example.pronedvizapp.notifications.NotificationApp
import com.example.pronedvizapp.requests.ServerApiUsers
import com.example.pronedvizapp.requests.models.Statistics
import com.example.pronedvizapp.requests.models.Task
import com.example.pronedvizapp.requests.models.User
import com.example.pronedvizapp.requests.models.WorkTasksTypes
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.coroutineScope
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var fragmentManager: FragmentManager

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fragmentManager = supportFragmentManager

        createNotificationChannels(this)

        initUi()

        binding.bottomMenu.selectedItemId = R.id.bottomMenuItemMain

        NotesFragment.getCompletedTasksCurrentUser(this) { completedTasks ->
            completedTasks.let { arr -> arr.forEach { showResultDialog(it, this) } }
        }

        planPeriodicWork()
        //setMorningAlarm()
    }

    private fun setMorningAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this.applicationContext, NotificationApp::class.java).let {
                intent ->
            intent.putExtra("TITLE", "Напоминание")
            intent.putExtra("CONTENT", "Пора зайти в приложение и решить, чем сегодня заниматься")
            PendingIntent.getBroadcast(this.applicationContext, 480, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        var time = Calendar.getInstance()
        time.set(Calendar.HOUR_OF_DAY, 12)
        time.set(Calendar.MINUTE, 39)
        time.set(Calendar.SECOND, 0)
        if (time.timeInMillis < Calendar.getInstance().timeInMillis) {
            //time.add(Calendar.DAY_OF_MONTH, 1)
        }
//        alarmManager.cancel(alarmIntent)
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, time.timeInMillis, AlarmManager.INTERVAL_DAY, alarmIntent)
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun planPeriodicWork() {
        val workManager = WorkManager.getInstance(this.applicationContext)

        workManager.getWorkInfosByTagLiveData(GeoService.DEBUG_TAG).observe(this) { workInfos ->
            if (workInfos.isEmpty()) {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .setRequiresDeviceIdle(false)
                    .setRequiresCharging(false)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<GeoWorker>(60, TimeUnit.MINUTES)
                    .setInitialDelay(5, TimeUnit.SECONDS)
                    .setConstraints(constraints)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    GeoService.DEBUG_TAG,
                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                    workRequest
                )
                Log.d(GeoService.DEBUG_TAG, "scheduled")

            } else {
                Log.d(GeoService.DEBUG_TAG, "Work already scheduled")
            }
        }

//        val alarmManager = this.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//        val intent = Intent(this.applicationContext, GeoServiceBroadcast::class.java)
//        val alarmIntent = PendingIntent.getBroadcast(
//            this.applicationContext,
//            0, intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        alarmManager.setExactAndAllowWhileIdle(
//            AlarmManager.RTC_WAKEUP,
//            System.currentTimeMillis(),
//            alarmIntent
//        )
//        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(System.currentTimeMillis() + 5000L, alarmIntent), alarmIntent)
//        Log.d(GeoService.DEBUG_TAG, "planned ${alarmManager.nextAlarmClock}")
    }

    private fun initUi() {
        binding.gradientView.animateGradientColors()

        binding.bottomMenu.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED

        binding.addNewNoteFloatingActionButton.setOnClickListener {
//            loadFragment(CreateEditNoteFragment())
            showFragment("CreateEditNoteFragment")
        }

        binding.bottomMenu.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.bottomMenuItemWork -> {
//                    loadFragment(WorkFragment())
                    showFragment("WorkFragment")
                    true
                }

                R.id.bottomMenuItemMain -> {
//                    loadFragment(ProfileFragment())
                    showFragment("ProfileFragment")
                    true
                }

                R.id.bottomMenuItemNotes -> {
//                    loadFragment(NotesFragment())
                    showFragment("NotesFragment")
                    true
                }

                R.id.bottomMenuItemProfile -> {
//                    loadFragment(MainFragment())
                    showFragment("MainFragment")
                    true
                }

                else -> {
                    false
                }
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.mainContentFrame, fragment)
        transaction.commit()
    }

    private fun showFragment(tag: String) {
        val fragmentTransaction = fragmentManager.beginTransaction()

        val existingFragment = fragmentManager.findFragmentByTag(tag)
        if (existingFragment != null) {
            fragmentTransaction.show(existingFragment)
        } else {
            val newFragment = when (tag) {
                "ProfileFragment" -> ProfileFragment()
                "WorkFragment" -> WorkFragment()
                "NotesFragment" -> NotesFragment()
                "MainFragment" -> MainFragment()
                "CreateEditNoteFragment" -> CreateEditNoteFragment()
                else -> null
            }

            if (newFragment != null) {
                fragmentTransaction.add(R.id.mainContentFrame, newFragment, tag)
            }
        }

        for (fragment in fragmentManager.fragments) {
            if (fragment.tag != tag) {
                fragmentTransaction.hide(fragment)
            }
        }

        fragmentTransaction.commit()
    }

    override fun onResume() {
        super.onResume()
        binding = ActivityMainBinding.inflate(layoutInflater)
    }

    companion object {

        public fun createNotificationChannels(context: Context)
        {
            val nameNI = context.resources.getString(R.string.NOT_IMPORTANT_CHANNEL_NAME)
            val descNI = context.resources.getString(R.string.NOT_IMPORTANT_CHANNEL_DESCRIPTION)
            val importanceNI = NotificationManager.IMPORTANCE_HIGH

            val channelNI = NotificationChannel(nameNI, nameNI, importanceNI)

            channelNI.description = descNI

            val nameI = context.resources.getString(R.string.IMPORTANT_CHANNEL_NAME)
            val descI = context.resources.getString(R.string.IMPORTANT_CHANNEL_DESCRIPTION)
            val importanceI = NotificationManager.IMPORTANCE_HIGH

            val channelI = NotificationChannel(nameI, nameI, importanceI)

            channelI.description = descI

            val notificationManager = context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channelNI)
            notificationManager.createNotificationChannel(channelI)
        }

        public fun showResultDialog(currentTast: Task, context: Context) {

            val dialogBinding = FragmentActionResultBinding.inflate(LayoutInflater.from(context))

            dialogBinding.aboutActivityDescTextView1.setText("Как вы оцениваете результат своей работы в области ${currentTast.work_type}\n(${currentTast.desc})\n")
            dialogBinding.aboutActivityDescTextView2.setText("Отметьте нужные по вашему мнению пункты")

            if (currentTast.work_type == WorkTasksTypes.FLYERS.description || currentTast.work_type == WorkTasksTypes.CALLS.description) {
                dialogBinding.countNumberPicker.visibility = View.VISIBLE
                dialogBinding.countNumberPicker.maxValue = 50
                dialogBinding.countNumberPicker.minValue = 0
                dialogBinding.countNumberPicker.value = 1
            } else if (currentTast.work_type == WorkTasksTypes.DEAL.description || currentTast.work_type == WorkTasksTypes.DEPOSIT.description || currentTast.work_type == WorkTasksTypes.MEET.description || currentTast.work_type == WorkTasksTypes.SHOW.description) {
                dialogBinding.isConractSignedCheckBox.visibility = View.VISIBLE
            } else {
                dialogBinding.resultImageView.visibility = View.VISIBLE
            }

            val dialog = Dialog(context)
            dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
            dialog.setContentView(dialogBinding.root)

            var addValue = 1

            dialogBinding.goodActivityButton.setOnClickListener {
                if (currentTast.work_type == WorkTasksTypes.FLYERS.description || currentTast.work_type == WorkTasksTypes.CALLS.description) {
                    addValue = dialogBinding.countNumberPicker.value
                }
                MainActivity.editUserStatistics(context, currentTast.work_type, addValue, MainStatic.currentToken!!)
                NotesFragment.deleteTask(context, MainStatic.currentToken!!, currentTast.id)
                dialog.dismiss()
            }

            dialogBinding.badActivityButton.setOnClickListener {
                dialog.dismiss()
                NotesFragment.deleteTask(context, MainStatic.currentToken!!, currentTast.id)
                val intent = Intent(context, WebViewActivity::class.java)
                context.startActivity(intent)
            }

            dialog.show()
        }

        public suspend fun getUserStatistics(period: String, context: Context, token: String): Result<Statistics> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val usersApi = retrofit.create(ServerApiUsers::class.java)

            return@coroutineScope try {
                val response = usersApi.getStatisticsByPeriod(period, token).await()
                if (response != null) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Ошибка получения данных"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private fun editUserStatistics(context: Context, columnName: String, addValue: Int, token: String) {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val usersApi = retrofit.create(ServerApiUsers::class.java)

            val resp = usersApi.updateStatistics(columnName, addValue, token)
            resp.enqueue(object : Callback<Boolean> {
                override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                    if (response.isSuccessful) {

                    }
                }

                override fun onFailure(call: Call<Boolean>, t: Throwable) {

                }
            })
        }
    }
}