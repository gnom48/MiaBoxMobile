package com.example.pronedvizapp

import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.bisness.SharedPreferencesHelper
import com.example.pronedvizapp.bisness.calls.CallServiceWorker
import com.example.pronedvizapp.bisness.geo.GeoServiceWorker
import com.example.pronedvizapp.databinding.ActivityMainBinding
import com.example.pronedvizapp.databinding.FragmentDayResultsBinding
import com.example.pronedvizapp.main.CreateEditNoteFragment
import com.example.pronedvizapp.main.MainFragment
import com.example.pronedvizapp.main.NotesFragment
import com.example.pronedvizapp.main.ProfileFragment
import com.example.pronedvizapp.main.WorkFragment
import com.example.pronedvizapp.main.showResultDialog
import com.example.pronedvizapp.requests.RequestsRepository.getAllTasksCurrentUser
import com.example.pronedvizapp.requests.RequestsRepository.getUserStatisticsByPeriod
import com.example.pronedvizapp.requests.models.DAY_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.Statistics
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var fragmentManager: FragmentManager
    private val preferences: SharedPreferences by lazy { this.getSharedPreferences(SharedPreferencesHelper.SETTINGS_PREFS_KEY, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fragmentManager = supportFragmentManager
        createNotificationChannels(this)
        initUi()
        binding.bottomMenu.selectedItemId = R.id.bottomMenuItemMain
        MainStatic.isCurrentOnline.observe(this@MainActivity, Observer { isOnline ->
            if (isOnline) {
                binding.mainOfflineModeIndicatorLinearLayout.visibility = View.GONE
                binding.offlinePanelVisibility = View.GONE
            } else {
                binding.mainOfflineModeIndicatorLinearLayout.visibility = View.VISIBLE
                binding.offlinePanelVisibility = View.VISIBLE
            }
            // FIXME: не переотрисовывается
            binding.mainOfflineModeIndicatorLinearLayout.invalidate()
            binding.relativeContainer.invalidate()
            binding.mainContentFrame.invalidate()
        })

        lifecycleScope.launch {
            val completedTasks = getAllTasksCurrentUser(this@MainActivity, MainStatic.currentUser.id, MainStatic.currentToken, false)
            completedTasks.onSuccess { data ->
                data?.let { tasks ->
                    tasks.filter { item ->
                        val currentTimeSeconds = (System.currentTimeMillis() / 1000) + 10800
                        currentTimeSeconds > item.dateTime + item.durationSeconds
                    }
                    .forEach { task -> showResultDialog(task, this@MainActivity, this@MainActivity) }
                }
            }
            completedTasks.onFailure {

            }
            completedTasks.onCached { data ->
                data?.let { tasks ->
                    tasks.filter { item ->
                        val currentTimeSeconds = (System.currentTimeMillis() / 1000) + 10800
                        currentTimeSeconds > item.dateTime + item.durationSeconds
                    }
                    .forEach { task -> showResultDialog(task, this@MainActivity, this@MainActivity) }
                }
            }
        }

        GeoServiceWorker.schedulePeriodicWork(this)
        CallServiceWorker.schedulePeriodicWork(this)

        val lastEveningMessage = intent.getIntExtra(SharedPreferencesHelper.EVENING_MESSAGE_TAG, -1000000)
        val lastPrefEveningMessage = preferences.getInt(SharedPreferencesHelper.EVENING_MESSAGE_TAG, -1000000)
        if (lastEveningMessage > 0 || lastPrefEveningMessage >= (System.currentTimeMillis() / 1000) - (3 * 60 * 60)) {
            lifecycleScope.launch {
                val dayStatistics = getUserStatisticsByPeriod(DAY_STATISTICS_PERIOD, this@MainActivity, MainStatic.currentToken!!)
                dayStatistics.onSuccess { s: Statistics ->
                    showEveningMessage(s)
                }
                dayStatistics.onFailure { e ->
                    Log.e("EveningMessage", "getUserStatisticsByPeriod onFailure in eveningMessage", e)
                }
            }
        }
    }

    private fun showEveningMessage(stat: Statistics) {

        val editor = preferences.edit()
        editor.putInt(SharedPreferencesHelper.EVENING_MESSAGE_TAG, -100000).apply()

        val dialog = Dialog(this@MainActivity)

        val dialogBinding = FragmentDayResultsBinding.inflate(LayoutInflater.from(this@MainActivity))

        dialogBinding.resultsTextView.text = generateMotivationalResultMessage(stat.flyers, stat.calls, stat.dealsRent + stat.dealsSale > 0)

        dialogBinding.countAnalyticsTextView.text = stat.analytics.toString()
        dialogBinding.countCallsTextView.text = stat.calls.toString()
        dialogBinding.countFlyersTextView.text = stat.flyers.toString()
        dialogBinding.countDealsTextView.text = (stat.dealsSale + stat.dealsRent).toString()
        dialogBinding.subCountDealsSaleTextView.text = stat.dealsSale.toString()
        dialogBinding.subCountDealsRentTextView.text = stat.dealsRent.toString()
        dialogBinding.countMeetsTextView.text = stat.meets.toString()
        dialogBinding.countDepositsTextView.text = stat.deposits.toString()
        dialogBinding.countSearchTextView.text = stat.searches.toString()
        dialogBinding.countShowsTextView.text = stat.shows.toString()
        dialogBinding.countOthersTextView.text = stat.others.toString()

        dialogBinding.closeImageButton.setOnClickListener {
            dialog.dismiss()
        }

        if (stat.analytics == 0) {
            dialogBinding.aCont.visibility = View.GONE
        }
        if (stat.calls == 0) {
            dialogBinding.cCont.visibility = View.GONE
        }
        if (stat.flyers == 0) {
            dialogBinding.fCont.visibility = View.GONE
        }
        if (stat.dealsRent == 0 && stat.dealsSale == 0) {
            dialogBinding.dCont.visibility = View.GONE
        }
        if (stat.meets == 0) {
            dialogBinding.mCont.visibility = View.GONE
        }
        if (stat.deposits == 0) {
            dialogBinding.depCont.visibility = View.GONE
        }
        if (stat.searches == 0) {
            dialogBinding.searchCont.visibility = View.GONE
        }
        if (stat.shows == 0) {
            dialogBinding.shCont.visibility = View.GONE
        }
        if (stat.others == 0) {
            dialogBinding.oCont.visibility = View.GONE
        }

        dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
        dialog.setContentView(dialogBinding.root)
        dialog.show()
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

    fun showFragment(tag: String) {
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

        fun generateMotivationalResultMessage(flyers: Int, calls: Int, deals: Boolean): String {
            val flyersTarget = 50
            val callsTarget = 15

            val messageBuilder = StringBuilder()

            if (flyers >= flyersTarget) {
                messageBuilder.append("Вы сделали $flyers рассклеек. Это отлично! ")
            } else {
                if (flyers == 0) {
                    messageBuilder.append("Вы не сделали ни одной расклейки! Постарайтесь в следующий раз улучшить результат. ")
                } else {
                    messageBuilder.append("Вы сделали $flyers рассклеек. Это хорошо, но можно еще лучше. ")
                }
            }

            if (calls >= callsTarget) {
                messageBuilder.append("\nВы сделали $calls звонков. Это отлично! ")
            } else {
                if (calls == 0) {
                    messageBuilder.append("Вы не сделали ни одного холодного звонка! Задумайтесь над улучшением этого показателя в следующий раз. ")
                } else {
                    messageBuilder.append("\nВы сделали $calls звонков. Это хорошо, но можно еще лучше. ")
                }
            }

            if (deals) {
                messageBuilder.append("\nВы заключили сделку сегодня и это достойно уважения! ")
            } else {
                messageBuilder.append("\nСегодня не было сделок, но это не повод расстраиваться. Продолжайте трудиться в том же духе! ")
            }

            return messageBuilder.toString()
        }
    }
}