package com.example.pronedvizapp.bisness.calls

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.pronedvizapp.InitialActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.requests.ServerApiCalls
import com.example.pronedvizapp.requests.models.TranscriptionTask
import com.example.pronedvizapp.requests.models.TranscriptionTaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import okhttp3.RequestBody
import java.time.LocalDate

class CallRecordingService : Service() {

    private var phoneStateListener: PhoneStateListener? = null
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
                            try {
                                CoroutineScope(Dispatchers.IO).launch {
                                    delay(3000)
                                    val recordingFile = getLastCallRecordingFile(preferences.getString("RECORDINGS_PATH", null))
                                    Log.d(DEBUG_TAG, "файл: $recordingFile")
                                    if (recordingFile != null) {
                                        Log.d(DEBUG_TAG, "$recordingFile | $phoneNumber | ${(callEndTime - callStartTime).toInt()}")
                                        val result = uploadCallRecordAsync(
                                            recordingFile,
                                            phoneNumber!!,
                                            "no info",
                                            System.currentTimeMillis() / 1000,
                                            getContactName(phoneNumber, this@CallRecordingService.applicationContext),
                                            (callEndTime - callStartTime).toInt(),
                                            callType,
                                            this@CallRecordingService)
                                        if (result == null) {
                                            Log.e(CallRecordingService.DEBUG_TAG, "error uploadCallRecordAsync")
                                        } else {
                                            Log.d(CallRecordingService.DEBUG_TAG, "uploaded: $result")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(CallRecordingService.DEBUG_TAG, "error in CALL_STATE_IDLE")
                            }
                            callStartTime = 0
                            callEndTime = 0
                            callType = 0
                        }
                    }
                }
            }
        }

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        createNotificationChannel()
        startForeground(System.currentTimeMillis().toInt() / 1000, createNotification())
    }

    fun getContactName(phoneNumber: String, context: Context): String {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            var contactName: String = UNKNOWN_CALLER
            val cursor = context.contentResolver.query(uri, projection, null, null, null)

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (index < 0) {
                        return contactName
                    }
                    contactName = cursor.getString(index)
                }
                cursor.close()
            }

            return contactName
        } catch (e: Exception) {
            return UNKNOWN_CALLER
        }
    }

    private fun getLastCallRecordingFile(recordingsPath: String?): File? {
        if (recordingsPath == "" || recordingsPath == null) {
            return null
        }
        val directory = File(Environment.getExternalStorageDirectory(), recordingsPath) //"Recordings/Call")
        if (!directory.exists() || !directory.isDirectory) {
            return null
        }
        val files = directory.listFiles()
        if (files.isEmpty()) {
            Log.e(DEBUG_TAG, "Recording file not found within timeout")
            return null
        }
        return files.maxByOrNull { it.lastModified() }
    }

    private suspend fun getCurrentCallRecordFile(): File? {
        val directory = File(Environment.getExternalStorageDirectory(), "Recordings/Call")
        if (!directory.exists() || !directory.isDirectory) {
            return null
        }

        val timeoutMillis = 30000L
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            val files = directory.listFiles().toMutableList()
            files.sortByDescending { it.lastModified() }
            if (files != null) {
                for (file in files) {
                    val nowDate = LocalDate.now()
                    if (file.name.matches(Regex("Запись вызовов_*_${nowDate.year}${nowDate.monthValue}${nowDate.dayOfMonth}_\\d{6}.m4a"))) {
                        return file
                    }
                }
            }
            delay(5000)
        }

        Log.e(DEBUG_TAG, "Recording file not found within timeout")
        return null
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(DEBUG_TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
//        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
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

    companion object {

        public const val UNKNOWN_CALLER: String = "Неизвестный"

        public const val DEBUG_TAG: String = "CallRecordingService"

        suspend fun uploadCallRecordAsync(file: File, phoneNumber: String, info: String, dateTime: Long, contactName: String, lengthSeconds: Int, callType: Int, context: Context): Int? {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address) )//+ "?info=$info&phone_number=$phoneNumber&date_time=$dateTime&contact_name=$contactName&length_seconds=$lengthSeconds&call_type=$callType")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val token = preferences.getString("TOKEN", "")
            if (token != null) {
                val api = retrofit.create(ServerApiCalls::class.java)

                val requestFile = RequestBody.create(MediaType.parse("audio/*"), file) // video
                val part = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = api.addCallInfoParams(part, info, phoneNumber, dateTime, contactName, lengthSeconds, callType, token)
                if (response.isSuccessful) {
                    return response.body()
                }
            }
            return null
        }

        suspend fun orderCallTranscription(context: Context, userId: Int, recordId: Int, model: String = "base", tokenAuthorization: String?): Result<TranscriptionTask?> =
            coroutineScope {
                val retrofit = Retrofit.Builder()
                    .baseUrl(context.getString(R.string.server_ip_address))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val callsApi = retrofit.create(ServerApiCalls::class.java)

                return@coroutineScope try {
                    val resp = callsApi.orderCallTranscription(userId, recordId, model, tokenAuthorization)
                    if (resp.isSuccessful) {
                        val response = resp.body()
                        Result.success(response)
                    } else {
                        Result.failure(Exception("Response is not successful: ${resp.code()}"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        suspend fun getOrderTranscriptionStatus(context: Context, taskId: String, tokenAuthorization: String?): Result<TranscriptionTaskStatus?> =
            coroutineScope {
                val retrofit = Retrofit.Builder()
                    .baseUrl(context.getString(R.string.server_ip_address))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val callsApi = retrofit.create(ServerApiCalls::class.java)

                return@coroutineScope try {
                    val resp = callsApi.getOrderTranscriptionStatus(taskId, tokenAuthorization)
                    if (resp.isSuccessful) {
                        val response = resp.body()
                        Result.success(response)
                    } else {
                        Result.failure(Exception("Response is not successful: ${resp.code()}"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
    }
}

