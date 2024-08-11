import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pronedvizapp.InitialActivity
import com.example.pronedvizapp.MapActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.bisness.calls.CallRecordingService.Companion.DEBUG_TAG
import com.example.pronedvizapp.bisness.geo.GeoConsts.LAST_GEO_POINT_UNIX_PREF_TAG
import com.example.pronedvizapp.bisness.geo.GeoPositionService.Companion.addAddressRecordAsync
import com.example.pronedvizapp.requests.models.AddressInfo
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset

// *************************************************** //
//                     Deprecated                      //
// *************************************************** //
// больше не нужен (но останется как источник кода)    //
// *************************************************** //
class GeoWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val geoService by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private val locationRequest by lazy { initLocationRequest() }

    private var mLocation: Location? = null
    private val preferences: SharedPreferences by lazy { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    private val notificationManager: NotificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val notificationId: Int by lazy { SystemClock.elapsedRealtime().toInt() }
    private val notificationChannelId = "geo_recording_channel"

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            notificationChannelId,
            "Geo Recording",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        createNotificationChannel()
        val intent = Intent(context, InitialActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, notificationChannelId)
            .setContentTitle("Geo Recording")
            .setContentText("Геолокация считалась")
            .setSmallIcon(R.drawable.on_work_task_icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun initLocationRequest(): LocationRequest {
        var request = LocationRequest.create()
        return request.apply {
            interval = 100
            fastestInterval = 70
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
    }

    private val geoCallback = object : LocationCallback() {
        override fun onLocationResult(geo: LocationResult) {
            for(locationResult in geo.locations) {
                mLocation = locationResult
                Log.d(DEBUG_TAG, "stop with mLocation value $mLocation")
            }
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

        if (LocalDateTime.now().toLocalTime().hour > 19 || LocalDateTime.now().toLocalTime().hour < 9) {
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

        geoService.requestLocationUpdates(locationRequest, geoCallback, Looper.getMainLooper())

        notificationManager.notify(notificationId, createNotification())

        return withContext(Dispatchers.IO) {
            try {
                while (mLocation == null) {
                    Thread.sleep(10)
                }
                val isSuccess = doMainWork(mLocation!!.latitude, mLocation!!.longitude)
                if (isSuccess) {
                    notificationManager.cancel(notificationId)
                    geoService.removeLocationUpdates(geoCallback)
                    Result.success()
                } else {
                    notificationManager.cancel(notificationId)
                    geoService.removeLocationUpdates(geoCallback)
                    Result.retry()
                }
            } catch (e: Exception) {
                notificationManager.cancel(notificationId)
                geoService.removeLocationUpdates(geoCallback)
                Result.failure()
            }
        }
    }

    private suspend fun doMainWork(lat: Double, lon: Double): Boolean {
        var isSuccessWork = false
        val maxRetries = 2
        var retryCount = 0

        while (retryCount < maxRetries || isSuccessWork) {
            if (isSuccessWork) {
                return true
            }
            val addressResponse = MapActivity.getAddressByCoordsAsync(context, mLocation!!)
            addressResponse.onSuccess { address ->
                Log.d(DEBUG_TAG, "addressResponse.onSuccess $address")
                if (address != null && address.suggestions.isNotEmpty()) {
                    val serverApiAddressAdditionResponse = addAddressRecordAsync(context, AddressInfo(
                        -1,
                        preferences.getInt("USER_ID", -1),
                        address.suggestions[0].value,
                        lat.toFloat(),
                        lon.toFloat(),
                        LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toInt()
                    ))
                    serverApiAddressAdditionResponse.onSuccess {  res ->
                        Log.d(DEBUG_TAG, "serverApiAddressAdditionResponse.onSuccess $res")
                        if (res != null && res > 0) {
                            isSuccessWork = true
                            val editor = preferences.edit()
                            editor.putLong(LAST_GEO_POINT_UNIX_PREF_TAG, System.currentTimeMillis() / 1000).apply()
                        }
                    }
                    serverApiAddressAdditionResponse.onFailure { e ->
                        Log.e(DEBUG_TAG, "serverApiAddressAdditionResponse.onFailure ${e.message}")
                    }
                }
            }
            addressResponse.onFailure { err ->
                Log.e(DEBUG_TAG, "addressResponse.onFailure ${err.message}")
            }
            retryCount++
        }

        return false
    }
}
