package com.example.pronedvizapp

import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.example.pronedvizapp.bisness.geo.GeoServiceWorker
import com.example.pronedvizapp.databinding.ActivityMainBinding
import com.example.pronedvizapp.databinding.FragmentActionResultBinding
import com.example.pronedvizapp.main.CreateEditNoteFragment
import com.example.pronedvizapp.main.MainFragment
import com.example.pronedvizapp.main.NotesFragment
import com.example.pronedvizapp.main.ProfileFragment
import com.example.pronedvizapp.main.WorkFragment
import com.example.pronedvizapp.notifications.FirebaseInstanceIdService
import com.example.pronedvizapp.requests.ServerApiUsers
import com.example.pronedvizapp.requests.models.Note
import com.example.pronedvizapp.requests.models.Statistics
import com.example.pronedvizapp.requests.models.Task
import com.example.pronedvizapp.requests.models.WorkTasksTypes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.coroutineScope
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.ZoneOffset

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
            completedTasks.let { arr -> arr.forEach { showResultDialog(it, this, this) } }
        }

//        FirebaseMessaging.getInstance().subscribeToTopic("broadcast")
//            .addOnCompleteListener { task ->
//                var msg = "msg_subscribed"
//                if (!task.isSuccessful) {
//                    msg = "msg_subscribe_failed"
//                }
//                Log.d(FirebaseInstanceIdService.DEBUG_TAG, msg)
//            }

        GeoServiceWorker.schedulePeriodicWork(this)
    }

    private fun initUi() {
        binding.gradientView.animateGradientColors()

        binding.bottomMenu.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED

        binding.addNewNoteFloatingActionButton.setOnClickListener {
            showFragment("CreateEditNoteFragment")
        }

        binding.bottomMenu.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.bottomMenuItemWork -> {
                    showFragment("WorkFragment")
                    true
                }

                R.id.bottomMenuItemMain -> {
                    showFragment("ProfileFragment")
                    true
                }

                R.id.bottomMenuItemNotes -> {
                    showFragment("NotesFragment")
                    true
                }

                R.id.bottomMenuItemProfile -> {
                    showFragment("MainFragment")
                    true
                }

                else -> {
                    false
                }
            }
        }
    }

    private fun showFragment(tag: String) {
        val fragmentTransaction = this.fragmentManager.beginTransaction()

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

        public fun createNotificationChannels(context: Context) {
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

        fun showResultDialog(task: Task, context: Context, owner: MainActivity) {
            val dialogBinding = FragmentActionResultBinding.inflate(LayoutInflater.from(context))

            dialogBinding.aboutActivityDescTextView1.text = "Как вы оцениваете результат своей работы в области \"${task.workType}\" (${task.desc})?"

            if (task.workType == WorkTasksTypes.FLYERS.description || task.workType == WorkTasksTypes.CALLS.description) {
                dialogBinding.aboutActivityDescTextView2.text = "Введите количественный показатель:"
                dialogBinding.countEditText.visibility = View.VISIBLE
//                dialogBinding.countNumberPicker.visibility = View.VISIBLE

            } else if (task.workType == WorkTasksTypes.DEPOSIT.description || task.workType == WorkTasksTypes.MEET.description || task.workType == WorkTasksTypes.SHOW.description) {
                dialogBinding.aboutActivityDescTextView2.text = "Как прошло общение с клиентом:"
                dialogBinding.isConractSignedCheckBox.visibility = View.VISIBLE
            } else {
                dialogBinding.aboutActivityDescTextView2.text = "Каков результат сделки:"
//                dialogBinding.resultImageView.visibility = View.VISIBLE
            }

            val dialog = Dialog(context)
            dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
            dialog.setContentView(dialogBinding.root)

            var addValue = 1

            dialogBinding.goodActivityButton.setOnClickListener {
                if (task.workType == WorkTasksTypes.FLYERS.description || task.workType == WorkTasksTypes.CALLS.description) {
                    addValue = dialogBinding.countEditText.text.toString().toInt()
//                    addValue = dialogBinding.countNumberPicker.value
                } else if (task.workType == WorkTasksTypes.DEPOSIT.description || task.workType == WorkTasksTypes.MEET.description || task.workType == WorkTasksTypes.SHOW.description) {
                    addValue = if (dialogBinding.isConractSignedCheckBox.isChecked) 1 else 0
                }
                MainActivity.editUserStatistics(context, task.workType, addValue, MainStatic.currentToken!!)
                NotesFragment.deleteTask(context, MainStatic.currentToken!!, task.id)
                dialog.dismiss()
            }

            dialogBinding.badActivityButton.setOnClickListener {
                dialog.dismiss()
                NotesFragment.deleteTask(context, MainStatic.currentToken!!, task.id)

                MaterialAlertDialogBuilder(context)
                    .setMessage("Рекомендуется почитать обучающие материалы для того, чтобы в следующий раз достигнуть успеха.")
                    .setPositiveButton("Да, спасибо") { materialAlertDialog, _ ->
                        materialAlertDialog.dismiss()
                        val intent = Intent(context, WebViewActivity::class.java)
                        intent.putExtra(WebViewActivity.SOURCE, "baza.html")
                        context.startActivity(intent)
                    }
                    .create()
                    .show()
            }

            dialogBinding.postpondActivityButton.setOnClickListener {
                dialog.dismiss()
                MaterialAlertDialogBuilder(context)
                    .setMessage("Рекомендуется сделать заметку, чтобы не забыть о том, что встреча перенесена.")
                    .setPositiveButton("Да") { materialAlertDialog, _ ->
                        materialAlertDialog.dismiss()
//                        owner.showFragment("CreateEditNoteFragment")
                        val fragment = CreateEditNoteFragment(Note(0, "${task.workType} с ...", "По предмету ...", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC), MainStatic.currentUser!!.id, 0))
                        val transaction = owner.supportFragmentManager.beginTransaction()
                        transaction.replace(R.id.mainContentFrame, fragment)
                        transaction.commit()
                    }
                    .setNegativeButton("Нет, спасибо") { materialAlertDialog, _ ->
                        materialAlertDialog.dismiss()
                    }
                    .create()
                    .show()
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