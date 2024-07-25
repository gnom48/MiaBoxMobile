package com.example.pronedvizapp.bisness.calls

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import okhttp3.RequestBody
class CallRecordingService : Service() {

    private var phoneStateListener: PhoneStateListener? = null
    //private var callRecorder: CallRecorder? = null
    private val notificationChannelId = "call_recording_channel"
    private var lastState: Int = -1

    private var callStartTime: Long = 0
    private var callEndTime: Long = 0
    private var callType: Int = 0

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
//                        callRecorder = CallRecorder(this@CallRecordingService)
//                        callRecorder?.startRecording()
                        lastState = TelephonyManager.CALL_STATE_OFFHOOK
                        callStartTime = System.currentTimeMillis() / 1000
                        callEndTime = 0
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (lastState == TelephonyManager.CALL_STATE_OFFHOOK){
                            callEndTime = System.currentTimeMillis() / 1000
                            Log.d(DEBUG_TAG, "Трубка положена")
//                            if (callRecorder != null) {
//                                callRecorder?.stopRecording()
//                            }
                            lastState = TelephonyManager.CALL_STATE_IDLE

                            Thread.sleep(3000) // ?
                            try {
                                val recordingFile = getLastCallRecordingFile()
                                if (recordingFile != null) {
                                    GlobalScope.launch {
                                        val result = uploadCallRecordAsync(
                                            recordingFile,
                                            phoneNumber!!,
                                            "info",
                                            System.currentTimeMillis(),
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
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        var contactName: String = "Неизвестный"
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
    }

    private fun getLastCallRecordingFile(): File? {
        val directory = File(Environment.getExternalStorageDirectory(), "Recordings/Call")
        if (!directory.exists() || !directory.isDirectory) {
            return null
        }
        val files = directory.listFiles()
        if (files.isEmpty()) {
            return null
        }
        val lastFile = files.maxByOrNull { it.lastModified() }
        return lastFile
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
            .setContentTitle("Call Recording")
            .setContentText("Рабочие звонки будут записываться")
            .setSmallIcon(R.drawable.on_work_task_icon)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {

        public const val DEBUG_TAG: String = "CallRecordingService"

        public fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }

        suspend fun uploadCallRecordAsync(file: File, phoneNumber: String, info: String, dateTime: Long, contactName: String, lengthSeconds: Int, callType: Int, context: Context): Int? {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val token = preferences.getString("TOKEN", "")
            if (token != null) {
                val api = retrofit.create(ServerApiCalls::class.java)

                val requestFile = RequestBody.create(MediaType.parse("audio/*"), file) // video
                val part = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val infoBody = RequestBody.create(MediaType.parse("text/plain"), info)
                val phoneNumberBody = RequestBody.create(MediaType.parse("text/plain"), phoneNumber)
                val dateTimeBody = RequestBody.create(MediaType.parse("text/plain"), dateTime.toString())
                val contactNameBody = RequestBody.create(MediaType.parse("text/plain"), contactName)
                val lengthSecondsBody = RequestBody.create(MediaType.parse("text/plain"), lengthSeconds.toString())
                val callTypeBody = RequestBody.create(MediaType.parse("text/plain"), callType.toString())

                val response = api.addCallInfo(part, infoBody, phoneNumberBody, dateTimeBody, contactNameBody, lengthSecondsBody, callTypeBody, token)
                if (response.isSuccessful) {
                    return response.body()
                }
            }
            return null
        }

    }
}

