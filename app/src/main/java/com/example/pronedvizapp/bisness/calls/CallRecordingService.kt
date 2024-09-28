package com.example.pronedvizapp.bisness.calls

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Environment
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.pronedvizapp.InitialActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.bisness.SharedPreferencesHelper
import com.example.pronedvizapp.bisness.network.NetworkListener
import com.example.pronedvizapp.requests.RequestsRepository.uploadCallRecordAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class CallRecordingService : Service() {

    private lateinit var phoneStateListener: PhoneStateListener
    private lateinit var networkListener: NetworkListener
    private val notificationChannelId = "call_recording_channel"
    private var lastState: Int = -1

    private var callStartTime: Long = 0
    private var callEndTime: Long = 0
    private var callType: Int = 0

    private val preferences: SharedPreferences by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        Log.d(DEBUG_TAG, "onCreate Сервис поднят")

        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        lastState = TelephonyManager.CALL_STATE_RINGING
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        lastState = TelephonyManager.CALL_STATE_OFFHOOK
                        callStartTime = System.currentTimeMillis() / 1000
                        callEndTime = 0
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (lastState == TelephonyManager.CALL_STATE_OFFHOOK){
                            callEndTime = System.currentTimeMillis() / 1000
                            Log.d(DEBUG_TAG, "Трубка положена")
                            lastState = TelephonyManager.CALL_STATE_IDLE

                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    delay(3000)
                                    val recordingFile = getLastCallRecordingFile(preferences.getString(SharedPreferencesHelper.CALLS_RECORDS_PATH_TAG, null))
                                    Log.d(DEBUG_TAG, "Файл: $recordingFile | $phoneNumber | ${(callEndTime - callStartTime).toInt()}")
                                    val result = uploadCallRecordAsync(
                                        file = recordingFile,
                                        phoneNumber = phoneNumber!!,
                                        info = if (recordingFile == null) "no record" else "no info",
                                        dateTime = System.currentTimeMillis() / 1000,
                                        contactName = CallsHelper.getContactName(phoneNumber, this@CallRecordingService.applicationContext),
                                        lengthSeconds = (callEndTime - callStartTime).toInt(),
                                        callType = callType,
                                        recordId = null,
                                        context = this@CallRecordingService
                                    )
                                    result.onSuccess {
                                        Log.d(CallRecordingService.DEBUG_TAG, "uploaded to server: $result")
                                        showInfoPush("Звонок добавлен", if (recordingFile == null) "(без записи)" else "(с записью)")
                                    }
                                    result.onCached {
                                        Log.d(CallRecordingService.DEBUG_TAG, "uploaded to local db: $result")
                                        showInfoPush("Звонок добавлен в локальное хранилище", if (recordingFile == null) "(без записи)" else "(с записью)")
                                    }
                                    result.onFailure {
                                        Log.e(CallRecordingService.DEBUG_TAG, "error uploadCallRecordAsync")
                                        showInfoPush("Звонок не записан", "Произошла ошибка:\nплохой ответ сервера")
                                    }
                                } catch (e: Exception) {
                                    Log.e(CallRecordingService.DEBUG_TAG, "error in CALL_STATE_IDLE: $e")
                                    showInfoPush("Звонок не записан", "Произошла ошибка:\n${e.message}")
                                }
                                callStartTime = 0
                                callEndTime = 0
                                callType = 0
                            }
                        }
                    }
                }
            }
        }

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        createNotificationChannel()
        startForeground(System.currentTimeMillis().toInt() / 1000, createNotification())

        networkListener = NetworkListener(this.applicationContext)
        networkListener.startListening(this.application)
    }

    private fun getLastCallRecordingFile(recordingsPath: String?): File? {
        Log.d(DEBUG_TAG, "files path: $recordingsPath")
        if (recordingsPath == "" || recordingsPath == null) {
            return null
        }
        val directory = File(Environment.getExternalStorageDirectory(), recordingsPath)
        if (!directory.exists() || !directory.isDirectory) {
            return null
        }
        var files = directory.listFiles()
        Log.d(DEBUG_TAG, "files in dir: ${files.map { it.name }}")
        if (files.isEmpty()) {
            Log.e(DEBUG_TAG, "Recording file not found within timeout")
            return null
        }
        files = files.filter { it.isFile }.toTypedArray()
        return files.maxByOrNull { it.lastModified() }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(DEBUG_TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        networkListener.stopListening()
        super.onDestroy()
        Log.d(DEBUG_TAG, "onDestroy Сервис закончен")
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            notificationChannelId,
            "Call Recording",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, InitialActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Запись звонков")
            .setContentText("Рабочие звонки будут записываться")
            .setSmallIcon(R.drawable.on_work_task_icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }

    private fun showInfoPush(title: String, message: String) {
        val notification = NotificationCompat.Builder(this@CallRecordingService.applicationContext, notificationChannelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.baseline_error_outline_24)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify((System.currentTimeMillis() / 1000).toInt(), notification)
    }

    companion object {

        public const val UNKNOWN_CALLER: String = "Неизвестный"

        public const val DEBUG_TAG: String = "CallRecordingService"

        fun getFileByName(filename: String?, recordingsPath: String?): File? {
            if (filename == null) {
                return null
            }
            Log.d(DEBUG_TAG, "files path: $recordingsPath")
            if (recordingsPath == "" || recordingsPath == null) {
                return null
            }
            val directory = File(Environment.getExternalStorageDirectory(), recordingsPath)
            if (!directory.exists() || !directory.isDirectory) {
                return null
            }

            val file = File(directory, filename)
            Log.d(DEBUG_TAG, "file: $file | ${file.exists()}")
            return if (file.isFile && file.exists()) file else null
        }
    }
}

