package com.example.pronedvizapp.bisness.geo

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.pronedvizapp.InitialActivity
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.bisness.geo.GeoService.Companion.DEBUG_TAG
import com.example.pronedvizapp.requests.DadataApi
import com.example.pronedvizapp.requests.ServerApiAddress
import com.example.pronedvizapp.requests.models.AddresInfo
import com.example.pronedvizapp.requests.models.AddressResponse
import com.example.pronedvizapp.requests.models.Coordinates
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GeoWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private lateinit var preferences: SharedPreferences

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

        preferences = this.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (preferences.contains(GeoService.LAST_GEO_POINT_UNIX_PREF_TAG)) {
            val whenLastQuery = preferences.getLong(GeoService.LAST_GEO_POINT_UNIX_PREF_TAG, 0L)
            if (((System.currentTimeMillis() / 1000) - whenLastQuery < 1 * 60 * 60 && whenLastQuery != 0L) ||
                (LocalDateTime.now().toLocalTime().hour > 19 || LocalDateTime.now().toLocalTime().hour < 7)) {
                Log.d(DEBUG_TAG, "too early to geo sync from worker")
                return Result.success()
            }
        } else {
            Log.e(DEBUG_TAG, "no prefs")
        }

        Log.d(DEBUG_TAG, "worker start")

        val intent = Intent(applicationContext, GeoService::class.java)
        ContextCompat.startForegroundService(applicationContext, intent)

        return Result.success()
    }
}

class GeoServiceBroadcast : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        Log.d(GeoService.DEBUG_TAG, "onReceive")
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(GeoService.DEBUG_TAG, "no permissions")
            return
        }
        Log.d(GeoService.DEBUG_TAG, "geo broadcast start")

        val serviceIntent = Intent(context, GeoService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
