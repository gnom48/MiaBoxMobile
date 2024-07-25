package com.example.pronedvizapp.bisness.geo

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pronedvizapp.InitialActivity
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.requests.DadataApi
import com.example.pronedvizapp.requests.ServerApiAddress
import com.example.pronedvizapp.requests.models.AddresInfo
import com.example.pronedvizapp.requests.models.AddressResponse
import com.example.pronedvizapp.requests.models.AddressSuggestion
import com.example.pronedvizapp.requests.models.Coordinates
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.search.SuggestResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class GeoService : Service() {

    private val notificationChannelId = "geo_recording_channel"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geoCallback: LocationCallback
    private var mLocation: Location? = null
    private lateinit var notificationManager: NotificationManager
    private val notificationId: Int by lazy { SystemClock.elapsedRealtime().toInt() }
    private lateinit var preferences: SharedPreferences
    private lateinit var context: Context

    override fun onCreate() {
        super.onCreate()
        Log.d(DEBUG_TAG, "onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(notificationId, notification)

        Log.d(DEBUG_TAG, "onStartCommand")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationUpdates()
        context = this.applicationContext

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            notificationChannelId,
            "Geo Recording",
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
            .setContentTitle("Geo Recording")
            .setContentText("Геолокация считалась")
            .setSmallIcon(R.drawable.on_work_task_icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 100
            fastestInterval = 30
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }

        var triesCount: Int = 0

        val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job)

        geoCallback = object : LocationCallback() {
            override fun onLocationResult(geo: LocationResult) {
                scope.launch {
                    for (location in geo.locations) {
                        if (triesCount > 2) {
                            Log.d(DEBUG_TAG, "stop by 3 failure")
                            job.cancel()
                            stopForeground(true)
                            stopSelf()
                            return@launch
                        }
                        mLocation = location
                        triesCount++
                        if (mLocation != null) {
                            doMainWork(mLocation!!.latitude, mLocation!!.longitude) { isSuccess ->
                                Log.d(DEBUG_TAG, "isDoWorkSuccess = $isSuccess")

                                if (isSuccess) {
                                    preferences = context.getSharedPreferences(
                                        "settings",
                                        Context.MODE_PRIVATE
                                    )
                                    val editor = preferences.edit()
                                    editor.putLong(LAST_GEO_POINT_UNIX_PREF_TAG, System.currentTimeMillis() / 1000).apply()

                                    fusedLocationClient.removeLocationUpdates(geoCallback)
                                    notificationManager.cancel(notificationId)
                                    stopForeground(true)
                                    stopSelf()
                                    Log.d(DEBUG_TAG, "stop")
                                }
                            }
                        }
                    }
                }
            }
        }


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            geoCallback,
            Looper.getMainLooper()
        )
    }

    private fun doMainWork(
        lat: Double,
        lon: Double,
        callback: (isSuccess: Boolean) -> Unit
    ) {
        Log.d(DEBUG_TAG, "doMainWork start")

        var isSuccessWork = false
        val maxRetries = 2
        var retryCount = 0

        while (retryCount < maxRetries) {
            val addressResponse = getAddressByCoordsSync(applicationContext, mLocation!!)
            Log.d(DEBUG_TAG, "$addressResponse by $mLocation")

            if (addressResponse != null && addressResponse?.suggestions!!.isNotEmpty()) {
                val serverApiAddressAdditionResponse = addAddressRecordSync(applicationContext, AddresInfo(
                    -1,
                    MainStatic.currentUser!!.id,
                    addressResponse.suggestions[0].value,
                    mLocation!!.latitude.toFloat(),
                    mLocation!!.longitude.toFloat(),
                    LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toInt()
                ))
                if (serverApiAddressAdditionResponse != null) {
                    isSuccessWork = true
                    break
                }
            } else {
                Log.e(DEBUG_TAG, "no dadata: $addressResponse")
            }

            retryCount++
        }

        //planNextWork()
        callback(isSuccessWork)
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun planNextWork() {
        val alarmManager = this.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this.applicationContext, GeoServiceBroadcast::class.java)
        val alarmIntent = PendingIntent.getBroadcast(
            this.applicationContext,
            0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
//        alarmManager.setExactAndAllowWhileIdle(
//            AlarmManager.RTC_WAKEUP,
//            System.currentTimeMillis() + (1000L * 60 * 5),
//            alarmIntent
//        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(System.currentTimeMillis() + (1000L * 60 * 5), alarmIntent), alarmIntent)
        Log.d(GeoService.DEBUG_TAG, "planned ${alarmManager.nextAlarmClock}")
    }

    override fun onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    companion object {

        const val DEBUG_TAG = "GeoService"
        const val LAST_GEO_POINT_UNIX_PREF_TAG = "LAST_GEO_POINT_UNIX"

        fun getAddressByCoordsSync(context: Context, mLocation: Location): AddressResponse? {
            val retrofit = Retrofit.Builder()
                .baseUrl("http://suggestions.dadata.ru/suggestions/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val dadataApi = retrofit.create(DadataApi::class.java)

            try {
                val response = dadataApi.getAddressByCoordinates(Coordinates(mLocation!!.latitude, mLocation!!.longitude)).execute()
                if (response.isSuccessful) {
                    return response.body()!!
                } else {
                    return null
                }
            } catch (e: Exception) {
                return null
            }
        }

        fun addAddressRecordSync(context: Context, addressInfo: AddresInfo): Int? {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val serverApiAddress = retrofit.create(ServerApiAddress::class.java)
            val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

            try {
                val response = preferences.getString("TOKEN", "")
                    ?.let { serverApiAddress.addAddressInfo(addressInfo, it).execute() }
                if (response!!.isSuccessful) {
                    return response.body()!!
                } else {
                    return null
                }
            } catch (e: Exception) {
                return null
            }
        }

        suspend fun addAddressRecordAsync(context: Context, addressInfo: AddresInfo): Result<Int?> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val serverApiAddress = retrofit.create(ServerApiAddress::class.java)
            val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

            return@coroutineScope try {
                val response = preferences.getString("TOKEN", "")?.let { serverApiAddress.addAddressInfo(addressInfo, it).await() }
                Result.success(response)
            } catch (e: Exception) {
                Log.e("MiaBox", "$e | $addressInfo")
                Result.failure(e)
            }
        }
    }
}
