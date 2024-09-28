package com.example.pronedvizapp.bisness.calls

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.pronedvizapp.bisness.calls.CallRecordingService.Companion.DEBUG_TAG
import com.example.pronedvizapp.bisness.isServiceRunning
import java.util.concurrent.TimeUnit

class CallServiceWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d(DEBUG_TAG, "worker start")

        if (!isServiceRunning(applicationContext, CallRecordingService::class.java)) {
            Log.d(DEBUG_TAG, "service up")
            ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, CallRecordingService::class.java))
        } else {
            Log.d(DEBUG_TAG, "service already running")
        }

        return Result.success()
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

                    val workRequest = PeriodicWorkRequestBuilder<CallServiceWorker>(1, TimeUnit.HOURS)
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
