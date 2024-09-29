package com.example.pronedvizapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pronedvizapp.adapters.CallsAdapter
import com.example.pronedvizapp.bisness.SharedPreferencesHelper
import com.example.pronedvizapp.bisness.calls.CallInfo
import com.example.pronedvizapp.bisness.calls.CallRecordingService
import com.example.pronedvizapp.bisness.calls.CallsGroup
import com.example.pronedvizapp.databinding.ActivityCallsBinding
import com.example.pronedvizapp.requests.RequestsRepository.getAllCallsByUserId
import com.example.pronedvizapp.requests.RequestsRepository.getAllUserCallsRecords
import com.example.pronedvizapp.requests.RequestsRepository.usingLocalDataToast
import com.example.pronedvizapp.requests.models.CallRecord
import com.example.pronedvizapp.requests.models.UserCall
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class CallsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallsBinding
    var dataSource = ArrayList<UserCall>()
    private val preferences: SharedPreferences by lazy { this.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallsBinding.inflate(layoutInflater)
        setSupportActionBar(binding.constraintLayout)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setContentView(binding.root)

        binding.recyclerView.adapter = CallsAdapter(this, listOf(), listOf())
        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        binding.rootSwipeRefreshLayout.setOnRefreshListener {
            updateRecyclerViewAdapter()
        }

        binding.goBackPanel.setOnClickListener {
            this.finish()
        }

        updateRecyclerViewAdapter()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.calls_optional_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return  when (item.itemId) {
            R.id.callsRecordsPathMenuItem -> {
                openDirectoryPicker()
                true
            }
            else -> { true }
        }
    }

    private fun openDirectoryPicker() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Изменить источник записей")
            .setMessage("Вы можете выбрать папку, в которую сохраняются ваши записи телефонных разговоров.\nТекущий источник: ${preferences.getString(SharedPreferencesHelper.CALLS_RECORDS_PATH_TAG, "не выбран")}")
            .setPositiveButton("Изменить") { _,_ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                }
                try {
                    startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE)
                } catch (e: Exception) {
                    Toast.makeText(this@CallsActivity, "Не удалось открыть проводник", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Ок") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private val OPEN_DIRECTORY_REQUEST_CODE = 1

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OPEN_DIRECTORY_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val selectedDir = DocumentFile.fromTreeUri(this, uri)
                    val selectedDirPath = selectedDir?.uri?.path
                    val path = selectedDirPath!!.split(":").reversed()[0]
                    val editor = preferences.edit()
                    editor.putString(SharedPreferencesHelper.CALLS_RECORDS_PATH_TAG, path).apply()
                    Log.d(CallRecordingService.DEBUG_TAG, "path changed: $path")
                } catch (e: Exception) {
                    Toast.makeText(this@CallsActivity, "Не удалось обновить путь", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateRecyclerViewAdapter() {
        refreshCalls { calls, records ->
            this@CallsActivity.dataSource = calls
            binding.recyclerView.layoutManager = LinearLayoutManager(this@CallsActivity, LinearLayoutManager.VERTICAL, false)
            val callsGroups = groupCallsByDate(calls)
            binding.recyclerView.adapter = CallsAdapter(this, callsGroups, records)

            if (calls.isEmpty() || calls == null) {
                binding.recyclerView.setBackgroundResource(R.drawable.no_data_img_background)
            } else {
                binding.recyclerView.setBackgroundColor(Color.TRANSPARENT)
            }
            binding.rootSwipeRefreshLayout.isRefreshing = false
        }
    }

    private fun refreshCalls(callback: (ArrayList<UserCall>, ArrayList<CallRecord>) -> Unit) {
        lifecycleScope.launch {
            val allCalls = getAllCallsByUserId(
                this@CallsActivity,
                MainStatic.currentUser!!.id,
                MainStatic.currentToken!!
            )

            allCalls.onSuccess { userCalls ->
                if (userCalls != null) {
                    val allRecords = getAllUserCallsRecords(
                        this@CallsActivity,
                        MainStatic.currentUser!!.id,
                        MainStatic.currentToken!!
                    )

                    allRecords.onSuccess {  userRecords ->
                        if (userRecords != null) {
                            callback(userCalls, userRecords as ArrayList<CallRecord>)
                        } else {
                            callback(userCalls, arrayListOf())
                        }
                    }
                    // здесь нет смысла обрабатывать onCached
                    allRecords.onFailure {
                        callback(userCalls, arrayListOf())
                    }

                } else {
                    callback(arrayListOf(), arrayListOf())
                }
            }
            allCalls.onCached { userCalls ->
                usingLocalDataToast(this@CallsActivity)
                if (userCalls != null) {
                    val allRecords = getAllUserCallsRecords(
                        this@CallsActivity,
                        MainStatic.currentUser!!.id,
                        MainStatic.currentToken!!
                    )

                    // здесь нет смысла обрабатывать onSucces
                    allRecords.onCached {  userRecords ->
                        if (userRecords != null) {
                            callback(userCalls, userRecords as ArrayList<CallRecord>)
                        } else {
                            callback(userCalls, arrayListOf())
                        }
                    }
                    allRecords.onFailure {
                        callback(userCalls, arrayListOf())
                    }

                } else {
                    callback(arrayListOf(), arrayListOf())
                }
            }
            allCalls.onFailure {
                Toast.makeText(this@CallsActivity, "Не удалось обновить: ${it.message}", Toast.LENGTH_SHORT).show()
                callback(arrayListOf(), arrayListOf())
            }
        }
    }

    companion object {

        public fun groupCallsByDate(calls: List<UserCall>): List<CallsGroup> {
            val callsByDate = mutableMapOf<LocalDate, MutableList<CallInfo>>()

            for (call in calls) {
                val localDateTime = LocalDateTime.ofEpochSecond(call.dateTime.toLong(), 0, ZoneOffset.UTC)
                localDateTime.plusHours(3)
                val localDate = localDateTime.toLocalDate()
                if (!callsByDate.containsKey(localDate)) {
                    callsByDate[localDate] = mutableListOf()
                }
                callsByDate[localDate]?.add(
                    CallInfo(
                        lengthSeconds = call.lengthSeconds,
                        callerName = call.contactName,
                        dateTime = call.dateTime,
                        transcription = call.transcription,
                        phoneNumber = call.phoneNumber,
                        userId = call.userId,
                        recordId = call.recordId,
                        callType = call.callType
                    )
                )
            }

            val res = callsByDate.map { (date, calls) -> CallsGroup(date, calls) }
            return res.sortedByDescending { it.date }
        }
    }
}