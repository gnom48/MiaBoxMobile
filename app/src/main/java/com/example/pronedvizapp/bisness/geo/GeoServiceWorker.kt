package com.example.pronedvizapp.bisness.geo

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pronedvizapp.bisness.calls.CallRecordingService
import com.example.pronedvizapp.bisness.geo.GeoConsts.DEBUG_TAG
import com.example.pronedvizapp.bisness.geo.GeoConsts.LAST_GEO_POINT_UNIX_PREF_TAG
import com.example.pronedvizapp.bisness.geo.GeoConsts.MSG_GET_LOCATION
import com.example.pronedvizapp.bisness.isServiceRunning
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class GeoServiceWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val preferences: SharedPreferences by lazy { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    private var messenger: Messenger? = null
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(DEBUG_TAG, "onServiceСonnected")
            messenger = Messenger(service)
            bound = true
            sendMessageToService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(DEBUG_TAG, "onServiceDisconnected")
            messenger = null
            bound = false
        }
    }

    override suspend fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }

        if (LocalDateTime.now().toLocalTime().hour > 19 || LocalDateTime.now().toLocalTime().hour < 7) {
            return Result.success()
        }

        if (preferences.contains(LAST_GEO_POINT_UNIX_PREF_TAG)) {
            val whenLastQuery = preferences.getLong(LAST_GEO_POINT_UNIX_PREF_TAG, 0L)
            if (((System.currentTimeMillis() / 1000) - whenLastQuery < 1 * 60 * 60 && whenLastQuery != 0L) ||
                (LocalDateTime.now().toLocalTime().hour > 19 || LocalDateTime.now().toLocalTime().hour < 7)) {
                Log.d(DEBUG_TAG, "too early to geo sync from worker")
                return Result.success()
            }
        } else {
            Log.e(DEBUG_TAG, "no prefs")
        }

        Log.d(DEBUG_TAG, "worker start")

        if (!isServiceRunning(applicationContext, GeoPositionService::class.java)) {
            Log.d(DEBUG_TAG, "service up")
            ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, GeoPositionService::class.java))
        } else {
            Log.d(DEBUG_TAG, "service already running")
        }

        val intent = Intent(applicationContext, GeoPositionService::class.java)
        applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d(DEBUG_TAG, "bindService")
        return Result.success()
    }

    private fun sendMessageToService() {
        if (bound) {
            val message = Message.obtain(null, MSG_GET_LOCATION)
            messenger?.send(message)
        }
    }

    companion object {
        fun schedulePeriodicWork(owner: AppCompatActivity) {
            val workManager = WorkManager.getInstance(owner.applicationContext)

            workManager.getWorkInfosByTagLiveData(DEBUG_TAG).observe(owner) { workInfos ->
                if (workInfos.isEmpty()) {
                    val constraints = Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .setRequiresDeviceIdle(false)
                        .setRequiresCharging(false)
                        .build()

                    val workRequest = PeriodicWorkRequestBuilder<GeoServiceWorker>(1, TimeUnit.HOURS)
                        .setInitialDelay(5, TimeUnit.SECONDS)
                        .setConstraints(constraints)
                        .build()

                    workManager.enqueueUniquePeriodicWork(
                        DEBUG_TAG,
                        ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                        workRequest
                    )
                    Log.d(DEBUG_TAG, "scheduled")

                } else {
                    Log.d(DEBUG_TAG, "Work already scheduled")
                }
            }

        }
    }
}
