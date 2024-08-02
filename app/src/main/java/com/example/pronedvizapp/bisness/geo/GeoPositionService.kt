package com.example.pronedvizapp.bisness.geo

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.pronedvizapp.MapActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.bisness.geo.GeoConsts.CHANNEL_ID
import com.example.pronedvizapp.bisness.geo.GeoConsts.CHANNEL_NAME
import com.example.pronedvizapp.bisness.geo.GeoConsts.DEBUG_TAG
import com.example.pronedvizapp.bisness.geo.GeoConsts.LAST_GEO_POINT_UNIX_PREF_TAG
import com.example.pronedvizapp.bisness.geo.GeoConsts.MSG_GET_LOCATION
import com.example.pronedvizapp.bisness.geo.GeoConsts.NOTIFICATION_ID
import com.example.pronedvizapp.requests.ServerApiAddress
import com.example.pronedvizapp.requests.models.AddresInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.ZoneOffset

class GeoPositionService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val messenger = Messenger(IncomingHandler())
    private val preferences: SharedPreferences by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        Log.d(DEBUG_TAG, "onCreate")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(DEBUG_TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(DEBUG_TAG, "onBind")
        return messenger.binder
    }

    private fun getCurrentLocation() {
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
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val requestsResult = withContext(Dispatchers.IO) {
                        val addressResponse = MapActivity.getAddressByCoordsAsync(this@GeoPositionService.applicationContext, location)
                        addressResponse.onSuccess { address ->
                            Log.d(DEBUG_TAG, "addressResponse.onSuccess $address")
                            if (address != null && address.suggestions.isNotEmpty()) {
                                val serverApiAddressAdditionResponse =
                                    addAddressRecordAsync(
                                        this@GeoPositionService.applicationContext, AddresInfo(
                                            -1,
                                            preferences.getInt("USER_ID", -1),
                                            address.suggestions[0].value,
                                            location.latitude.toFloat(),
                                            location.longitude.toFloat(),
                                            LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                                                .toInt()
                                        )
                                    )
                                serverApiAddressAdditionResponse.onSuccess {  res ->
                                    Log.d(DEBUG_TAG, "serverApiAddressAdditionResponse.onSuccess $res")
                                    if (res != null && res > 0) {
                                        val editor = preferences.edit()
                                        editor.putLong(LAST_GEO_POINT_UNIX_PREF_TAG, System.currentTimeMillis() / 1000).apply()
                                        return@withContext res
                                    }
                                }
                                serverApiAddressAdditionResponse.onFailure { e ->
                                    Log.e(DEBUG_TAG, "serverApiAddressAdditionResponse.onFailure ${e.message}")
                                    return@withContext -1
                                }
                            }
                        }
                        addressResponse.onFailure { err ->
                            Log.e(DEBUG_TAG, "addressResponse.onFailure ${err.message}")
                            return@withContext -1
                        }
                        return@withContext -1
                    }
                }
            } else {
                Log.d(DEBUG_TAG, "no coords")
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Служба геолокации запущена")
            .setContentText("Координаты будут записываться")
            .setSmallIcon(R.drawable.baseline_location_pin_24)
            .setOngoing(true)
            .setAutoCancel(false)
        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(NOTIFICATION_ID)
    }

    inner class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_GET_LOCATION -> {
                    Log.d(DEBUG_TAG, "MSG_GET_LOCATION handeled")
                    getCurrentLocation()
                }
            }
        }
    }

    companion object {

        suspend fun addAddressRecordAsync(context: Context, addressInfo: AddresInfo): Result<Int?> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val serverApiAddress = retrofit.create(ServerApiAddress::class.java)
            val preferences = context.getSharedPreferences("settings", MODE_PRIVATE)

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
