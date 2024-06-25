package com.example.pronedvizapp.bisness.calls

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.pronedvizapp.InitialActivity
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.R

//class CallRecordingService : Service() {
//
//    private var phoneStateListener: PhoneStateListener? = null
//
//    private var callRecorder: CallRecorder? = null
//
//    override fun onCreate() {
//        super.onCreate()
//        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//
//        Log.d("CallRecordingService", "Сервис поднят")
//
//        phoneStateListener = object : PhoneStateListener() {
//            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
//                super.onCallStateChanged(state, phoneNumber)
//                when (state) {
//                    TelephonyManager.CALL_STATE_RINGING -> {
//                        Log.d("CallRecordingService", "Звонок пошел")
//                        // запись начнется когда трубка будет снята
//                    }
//                    TelephonyManager.CALL_STATE_OFFHOOK -> {
//                        Log.d("CallRecordingService", "Трубка поднята")
//                        callRecorder = CallRecorder(this@CallRecordingService)
//                        callRecorder?.startRecording()
//                    }
//                    TelephonyManager.CALL_STATE_IDLE -> {
//                        Log.d("CallRecordingService", "Трубка положена")
//                        if (callRecorder != null) {
//                            callRecorder?.stopRecording()
//                        }
//                    }
//                }
//            }
//        }
//
//        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        Log.d("CallRecordingService", "onStartCommand")
//        return START_STICKY
//    }
//
//    override fun onBind(intent: Intent): IBinder? {
//        return null
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
//        Log.d("CallRecordingService", "Сервис переподнят")
//
//    }
//}

class CallRecordingService : Service() {

    private var phoneStateListener: PhoneStateListener? = null
    private var callRecorder: CallRecorder? = null
    private val notificationChannelId = "call_recording_channel"
    private var lastState: Int = -1

    override fun onCreate() {
        super.onCreate()
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        Log.d(DEBUG_TAG, "onCreate Сервис поднят")

        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        Log.d(DEBUG_TAG, "Звонок пошел")
                        // запись начнется когда трубка будет снята
                        lastState = TelephonyManager.CALL_STATE_RINGING
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        Log.d(DEBUG_TAG, "Трубка поднята")
                        callRecorder = CallRecorder(this@CallRecordingService)
                        callRecorder?.startRecording()
                        lastState = TelephonyManager.CALL_STATE_OFFHOOK
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (lastState == TelephonyManager.CALL_STATE_OFFHOOK){
                            if (callRecorder != null) {
                                Log.d(DEBUG_TAG, "Трубка положена")
                                callRecorder?.stopRecording()
                            }
                            lastState = TelephonyManager.CALL_STATE_IDLE
                        }
                    }
                }
            }
        }

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        createNotificationChannel()
        startForeground(48, createNotification())
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

    }
}

