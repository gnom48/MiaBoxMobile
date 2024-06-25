package com.example.pronedvizapp.bisness.geo

import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.requests.DadataApi
import com.example.pronedvizapp.requests.ServerApiAddress
import com.example.pronedvizapp.requests.models.AddresInfo
import com.example.pronedvizapp.requests.models.AddressResponse
import com.example.pronedvizapp.requests.models.Coordinates
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GeoWorker(private var context: Context, workerParams: WorkerParameters): CoroutineWorker(context, workerParams) {

    private lateinit var mLocation: Location

    override suspend fun doWork(): Result {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY || (hour >= 22 || hour <= 6)) {
            //return Result.success()
        }

        val addressResult = withContext(Dispatchers.IO) {
            getAddressByCoords(context, mLocation)
        }

        if (addressResult.isSuccess) {
            val addressResponse = addressResult.getOrNull()
            if (addressResponse != null) {
                val serverApiAddressAdditionResponse = withContext(Dispatchers.IO) {
                    addAddressRecord(context, AddresInfo(
                        -1,
                        MainActivity.currentUser!!.id,
                        addressResponse.suggestions[0].value,
                        mLocation.latitude.toFloat(),
                        mLocation.longitude.toFloat(),
                        LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toInt()))
                }
            }
        } else {
            Toast.makeText(context, "Ошибка запроса к стороннему API", Toast.LENGTH_SHORT).show()
        }

        val workRequest = OneTimeWorkRequestBuilder<GeoWorker>()
            .setInitialDelay(10, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(GeoWorker.WORK_TAG, ExistingWorkPolicy.KEEP, workRequest)

        return Result.success()
    }

    private val geoCallback = object : LocationCallback() {
        override fun onLocationResult(geo: LocationResult) {
            for(locationResult in geo.locations) {
                mLocation = locationResult
            }
        }
    }

    companion object {

        const val WORK_TAG = "GeoPeriodicWorker"

        public suspend fun getAddressByCoords(context: Context, mLocation: Location): kotlin.Result<AddressResponse> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl("")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val dadataApi = retrofit.create(DadataApi::class.java)

            return@coroutineScope try {
                val response = dadataApi.getAddressByCoordinates(Coordinates(mLocation.latitude, mLocation.longitude)).await()
                if (response != null) {
                    kotlin.Result.success(response)
                } else {
                    kotlin.Result.failure(Exception("Ошибка получения данных"))
                }
            } catch (e: Exception) {
                kotlin.Result.failure(e)
            }
        }

        public suspend fun addAddressRecord(context: Context, addressInfo: AddresInfo): kotlin.Result<Int?> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl("")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val serverApiAddress = retrofit.create(ServerApiAddress::class.java)

            return@coroutineScope try {
                val response = serverApiAddress.addAddressInfo(addressInfo, MainActivity.currentToken!!).await()
                if (response != null) {
                    kotlin.Result.success(response)
                } else {
                    kotlin.Result.failure(Exception("Ошибка отправки данных"))
                }
            } catch (e: Exception) {
                kotlin.Result.failure(e)
            }
        }

    }
}
