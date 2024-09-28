package com.example.pronedvizapp.bisness.network

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.bisness.SharedPreferencesHelper
import com.example.pronedvizapp.bisness.calls.CallRecordingService
import com.example.pronedvizapp.databases.DbViewModel
import com.example.pronedvizapp.databases.models.CRUD
import com.example.pronedvizapp.databases.models.StatisticChangeOptionInfo
import com.example.pronedvizapp.requests.RequestsRepository
import com.example.pronedvizapp.requests.models.AddressInfo
import com.example.pronedvizapp.requests.models.CallRecord
import com.example.pronedvizapp.requests.models.Note
import com.example.pronedvizapp.requests.models.Statistics
import com.example.pronedvizapp.requests.models.Task
import com.example.pronedvizapp.requests.models.UserCall
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NetworkListener(private val context: Context) {
    private var isNetworkAvailable = false
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var dbViewModel: DbViewModel
    private val preferences: SharedPreferences by lazy { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    fun getIsNetworkAvailable() = isNetworkAvailable

    fun startListening(application: Application) {
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(networkReceiver, filter)
        dbViewModel = DbViewModel(application)

        Log.d(DEBUG_TAG, "NetworkListener startListening")

        scope.launch {
            while (true) {
                Log.d(DEBUG_TAG, "iteration")
                delay(900000) // 15 минут
                checkNetworkStatus()
                if (isNetworkAvailable) {
                    val login = preferences.getString(SharedPreferencesHelper.LAST_LOGIN_TAG, null)
                    val password = preferences.getString(SharedPreferencesHelper.LAST_PASSWORD_TAG, null)
                    if (login.isNullOrEmpty() || password.isNullOrEmpty()) {
                        stopListening()
                        break
                    } else {
                        val userId = preferences.getString(SharedPreferencesHelper.USER_ID_TAG, null)
                        if (dbViewModel.getChangesByUserIdAsync(userId.toString()).isEmpty()) {
                            delay(900000) // 15 минут
                            continue
                        }
                        RequestsRepository.authForToken(context, login, password)
                            .onSuccess { newToken ->
                                preferences.edit().putString(SharedPreferencesHelper.TOKEN_TAG, newToken).apply()
                                Log.d(DEBUG_TAG, "token refreshed")
                                sendPendingRequestsIfNeed()
                            }
                    }
                }
            }
        }
    }

    fun stopListening() {
        Log.d(DEBUG_TAG, "NetworkListener stopListening")
        context.unregisterReceiver(networkReceiver)
        scope.cancel()
    }

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            checkNetworkStatus()
        }
    }

    private fun checkNetworkStatus() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        isNetworkAvailable = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        Log.d(DEBUG_TAG, "checkNetworkStatus result = $isNetworkAvailable")

        MainStatic.isCurrentOnline.value = isNetworkAvailable

        if (isNetworkAvailable) {
            CoroutineScope(Dispatchers.Main).launch {
                val login = preferences.getString(SharedPreferencesHelper.LAST_LOGIN_TAG, null)
                val password = preferences.getString(SharedPreferencesHelper.LAST_PASSWORD_TAG, null)
                if (login.isNullOrEmpty() || password.isNullOrEmpty()) {
                    stopListening()
                    return@launch
                } else {
                    val userId = preferences.getString(SharedPreferencesHelper.USER_ID_TAG, null)
                    if (dbViewModel.getChangesByUserIdAsync(userId.toString()).isEmpty()) {
                        return@launch
                    }
                    RequestsRepository.authForToken(context, login, password)
                        .onSuccess { newToken ->
                            preferences.edit().putString(SharedPreferencesHelper.TOKEN_TAG, newToken).apply()
                            Log.d(DEBUG_TAG, "token refreshed")
                            sendPendingRequestsIfNeed()
                        }
                }
            }
        }
    }

    private suspend fun sendPendingRequestsIfNeed() {
        preferences.getString(SharedPreferencesHelper.USER_ID_TAG, null)?.let {userId ->
            Log.d(DEBUG_TAG, "userId = $userId")
            preferences.getString(SharedPreferencesHelper.TOKEN_TAG, null)?.let { token ->
                Log.d(DEBUG_TAG, "token = $token")
                val changes = dbViewModel.getChangesByUserIdAsync(userId)
                changes.forEach { change ->
                    try {
                        Log.d(DEBUG_TAG, "-- change = $change")
                        when (change.dataTypeName) {
                            Note::class.java.name -> {
                                when (change.action) {
                                    CRUD.INSERT -> {
                                        RequestsRepository.addNewNote(context, dbViewModel.getNote(userId, change.recordId).castByJsonTo(Note::class.java), token, false)
                                            .onFailure {

                                            }
                                            .onSuccess {
                                                dbViewModel.deleteChange(change)
                                            }
                                    }
                                    CRUD.UPDATE -> {
                                        RequestsRepository.editNote(context, dbViewModel.getNote(userId, change.recordId).castByJsonTo(Note::class.java), token, false)
                                            .onFailure {

                                            }
                                            .onSuccess {
                                                dbViewModel.deleteChange(change)
                                            }
                                    }
                                    CRUD.DELETE -> {
                                        RequestsRepository.deleteNoteAsync(context, change.recordId, token, false)
                                            .onFailure {

                                            }
                                            .onSuccess {
                                                dbViewModel.deleteChange(change)
                                            }
                                    }
                                }
                            }
                            Task::class.java.name -> {
                                when (change.action) {
                                    CRUD.INSERT -> {
                                        RequestsRepository.addNewTask(context, dbViewModel.getTask(userId, change.recordId).castByJsonTo(Task::class.java), token, false)
                                            .onFailure {

                                            }
                                            .onSuccess {
                                                dbViewModel.deleteChange(change)
                                            }
                                    }
                                    CRUD.DELETE -> {
                                        RequestsRepository.deleteTaskAsync(context, change.recordId, token, false)
                                            .onFailure {

                                            }
                                            .onSuccess {
                                                dbViewModel.deleteChange(change)
                                            }
                                    }
                                }
                            }
                            AddressInfo::class.java.name -> {
                                when (change.action) {
                                    CRUD.INSERT -> {
                                        RequestsRepository.addAddressRecordAsync(context, dbViewModel.getAddressInfo(userId, change.recordId).castByJsonTo(AddressInfo::class.java), false)
                                            .onFailure {

                                            }
                                            .onSuccess {
                                                dbViewModel.deleteChange(change)
                                            }
                                    }
                                }
                            }
                            UserCall::class.java.name -> {
                                when (change.action) {
                                    CRUD.INSERT -> {
                                        val callInfo = dbViewModel.getUserCall(userId, change.recordId)
                                        val callRecord = callInfo.recordId?.let { dbViewModel.getCallRecordAsync(it) }
                                        if (callRecord != null) {
                                            RequestsRepository.uploadCallRecordAsync(
                                                file = CallRecordingService.getFileByName(callRecord.name, preferences.getString(SharedPreferencesHelper.CALLS_RECORDS_PATH_TAG, null)),
                                                phoneNumber = callInfo.phoneNumber,
                                                info = callInfo.info,
                                                dateTime = callInfo.dateTime.toLong(),
                                                contactName = callInfo.contactName,
                                                lengthSeconds = callInfo.lengthSeconds,
                                                callType = callInfo.callType,
                                                context = context,
                                                recordId = callRecord.id,
                                                isFromUI = false
                                            )
                                                .onSuccess {
                                                    dbViewModel.deleteChange(change)
                                                    changes.firstOrNull { it.recordId == callRecord.id && it.dataTypeName == CallRecord::class.java.name }
                                                        ?.let { changeToDel ->
                                                            dbViewModel.deleteChange(changeToDel)
                                                        }
                                                }
                                                .onFailure {

                                                }
                                                .onCached {

                                                }
                                        }
                                    }
                                }
                            }
                            Statistics::class.java.name -> {
                                when (change.action) {
                                    CRUD.UPDATE -> {
                                        val optionalData = Gson().fromJson(change.optionalInfo!!, StatisticChangeOptionInfo::class.java)
                                        RequestsRepository.editUserStatisticsAsync(context, optionalData.fieldName, optionalData.addValue, token, false)
                                            .onFailure {

                                            }
                                            .onSuccess {
                                                dbViewModel.deleteChange(change)
                                            }
                                    }
                                }
                            }
                        }
                    } catch (e: NullPointerException) {
                        MainStatic.dbViewModel.deleteChange(change)
                    } catch (e: Exception) {
                        Log.e(DEBUG_TAG, "-- change sync error: $change ($e)")
                    }
                }
            }
        }
    }

    companion object {
        const val DEBUG_TAG: String = "NetworkListener"
    }
}
