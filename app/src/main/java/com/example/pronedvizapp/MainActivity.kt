package com.example.pronedvizapp

import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pronedvizapp.bisness.geo.GeoWorker
import com.example.pronedvizapp.databinding.ActivityMainBinding
import com.example.pronedvizapp.databinding.FragmentActionResultBinding
import com.example.pronedvizapp.main.CreateEditNoteFragment
import com.example.pronedvizapp.main.MainFragment
import com.example.pronedvizapp.main.NotesFragment
import com.example.pronedvizapp.main.ProfileFragment
import com.example.pronedvizapp.main.WorkFragment
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
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannels(this)

        initUi()

        binding.bottomMenu.selectedItemId = R.id.bottomMenuItemMain

        NotesFragment.getCompletedTasksCurrentUser(this) { completedTasks ->
            completedTasks?.let { it.forEach { showResultDialog(it as Task, this) } }
        }

        val workRequest = OneTimeWorkRequestBuilder<GeoWorker>()
            .setInitialDelay(10, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(GeoWorker.WORK_TAG, ExistingWorkPolicy.KEEP, workRequest)
    }

    private fun initUi() {
        binding.gradientView.animateGradientColors()

        binding.bottomMenu.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED

        binding.addNewNoteFloatingActionButton.setOnClickListener {
            loadFragment(CreateEditNoteFragment())
        }

        binding.bottomMenu.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.bottomMenuItemWork -> {
                    loadFragment(WorkFragment())
                    true
                }

                R.id.bottomMenuItemMain -> {
                    loadFragment(ProfileFragment())
                    true
                }

                R.id.bottomMenuItemNotes -> {
                    loadFragment(NotesFragment())
                    true
                }

                R.id.bottomMenuItemProfile -> {
                    loadFragment(MainFragment())
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

    override fun onResume() {
        super.onResume()
        binding = ActivityMainBinding.inflate(layoutInflater)
    }

    companion object {
        var currentUser: User? = null
        var currentToken: String? = null

        @RequiresApi(Build.VERSION_CODES.O)
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
                MainActivity.editUserStatistics(context, currentTast.work_type, addValue, MainActivity.currentToken!!)
                NotesFragment.deleteTask(context, MainActivity.currentToken!!, currentTast.id)
                dialog.dismiss()
            }

            dialogBinding.badActivityButton.setOnClickListener {
                dialog.dismiss()
                NotesFragment.deleteTask(context, MainActivity.currentToken!!, currentTast.id)
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