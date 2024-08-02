package com.example.pronedvizapp

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.adapters.AddressesAdapter
import com.example.pronedvizapp.bisness.geo.GeoPositionService
import com.example.pronedvizapp.databinding.ActivityMapBinding
import com.example.pronedvizapp.requests.DadataApi
import com.example.pronedvizapp.requests.ServerApiAddress
import com.example.pronedvizapp.requests.models.AddresInfo
import com.example.pronedvizapp.requests.models.AddressResponse
import com.example.pronedvizapp.requests.models.Coordinates
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class MapActivity : AppCompatActivity(), Session.SearchListener, UserLocationObjectListener, CameraListener {

    lateinit var binding: ActivityMapBinding

    private lateinit var selectedLocalDateTimeStartPeriod: LocalDateTime
    private lateinit var selectedLocalDateTimeEndPeriod: LocalDateTime

    private val geoService by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val locationRequest by lazy { initLocationRequest() }
    private lateinit var mLocation: Location
    private lateinit var locationMapKit: UserLocationLayer
    private lateinit var searchSession: Session
    private lateinit var searchManager: SearchManager

    private fun setApiKeyIfNotExists(savedInstanceState: Bundle?) {
        try {
            MapKitFactory.setApiKey(this.getString(R.string.map_api_key))
        } catch (e: AssertionError) {
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setApiKeyIfNotExists(savedInstanceState)
        } finally {
            MapKitFactory.initialize(this)
        }

        binding = ActivityMapBinding.inflate(layoutInflater)
        setSupportActionBar(binding.constraintLayout)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setContentView(binding.root)

        selectedLocalDateTimeStartPeriod = LocalDateTime.of(LocalDate.now(), LocalTime.of(0,0, 1))
        selectedLocalDateTimeEndPeriod = LocalDateTime.of(LocalDate.now(), LocalTime.of(23,59, 59))
        binding.addressesMapView.map.isNightModeEnabled = true

        binding.goBackPanel.setOnClickListener {
            this.finish()
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(this, InitialActivity::class.java)
            startActivity(intent)

            binding.addressesMapView.map.move(CameraPosition(Point(59.37177616183529, 28.60738007463778), 12.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 2.0f), null
            )
        }
        val mapKit = MapKitFactory.getInstance()

        locationMapKit = mapKit.createUserLocationLayer(binding.addressesMapView.mapWindow)
        locationMapKit.isVisible = true
        locationMapKit.isHeadingEnabled = true
        locationMapKit.setObjectListener(this)

        SearchFactory.initialize(this)
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        binding.addressesMapView.map.addCameraListener(this)

        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchSession = searchManager.submit(
                    binding.searchEditText.text.toString(),
                    VisibleRegionUtils.toPolygon(binding.addressesMapView.map.visibleRegion),
                    SearchOptions(),
                    this
                )
            }
            false
        }

        binding.setDateStartTextView.setOnClickListener {
            val year = selectedLocalDateTimeStartPeriod.year
            val month = selectedLocalDateTimeStartPeriod.monthValue - 1 // Месяцы в DatePickerDialog начинаются с 0
            val day = selectedLocalDateTimeStartPeriod.dayOfMonth

            val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, selectedLocalDateTimeStartPeriod.hour, selectedLocalDateTimeStartPeriod.minute, selectedLocalDateTimeStartPeriod.second)

                if (selectedDate.isAfter(selectedLocalDateTimeEndPeriod)) {
                    MaterialAlertDialogBuilder(this@MapActivity)
                        .setMessage("Извините, выберите дату начала периода раньше даты окончания!")
                        .setPositiveButton("Ок") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                    return@OnDateSetListener
                }

                selectedLocalDateTimeStartPeriod = selectedDate
                binding.setDateStartTextView.text = String.format(Locale.US, "%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear)
            }, year, month, day).apply {
                datePicker.maxDate = selectedLocalDateTimeEndPeriod.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            datePickerDialog.setTitle("Начало периода")
            datePickerDialog.show()
        }

        binding.setDateEndTextView.setOnClickListener {
            val year = selectedLocalDateTimeEndPeriod.year
            val month = selectedLocalDateTimeEndPeriod.monthValue - 1 // Месяцы в DatePickerDialog начинаются с 0
            val day = selectedLocalDateTimeEndPeriod.dayOfMonth

            val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, selectedLocalDateTimeEndPeriod.hour, selectedLocalDateTimeEndPeriod.minute, selectedLocalDateTimeEndPeriod.second)

                if (selectedDate.isBefore(selectedLocalDateTimeStartPeriod)) {
                    MaterialAlertDialogBuilder(this@MapActivity)
                        .setMessage("Извините, выберите дату окончания периода позже даты начала!")
                        .setPositiveButton("Ок") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                    return@OnDateSetListener
                }

                selectedLocalDateTimeEndPeriod = selectedDate
                binding.setDateEndTextView.text = String.format(Locale.US, "%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear)
            }, year, month, day).apply {
                datePicker.minDate = selectedLocalDateTimeStartPeriod.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            datePickerDialog.setTitle("Конец периода")
            datePickerDialog.show()
        }

        binding.showAddressesByPeriodTextView.setOnClickListener {
            it.isEnabled = false
            updateGeoPoints()
            it.isEnabled = true
        }

        binding.geopositionFloatingActionButton.setOnClickListener {
            try {
                val tPoint = Point(mLocation.latitude, mLocation.longitude)

                binding.addressesMapView.map.move(
                    CameraPosition(tPoint, 12.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 2.0f), null
                )
            } catch (e: Exception) {
                return@setOnClickListener
            }
        }

        binding.cancelSearchButton.setOnClickListener {
            binding.searchEditText.setText("")
            binding.addressesMapView.map.mapObjects.clear()
        }

        geoService.requestLocationUpdates(locationRequest, geoCallback, null)
    }

    private fun updateGeoPoints() {
        val mapObjects: MapObjectCollection = binding.addressesMapView.map.mapObjects
        mapObjects.clear()

        getAllAddresses(
            MainStatic.currentUser!!.id,
            selectedLocalDateTimeStartPeriod.toEpochSecond(ZoneOffset.UTC).toInt(),
            selectedLocalDateTimeEndPeriod.toEpochSecond(ZoneOffset.UTC).toInt(),
            this.applicationContext) {addresses ->

            if (addresses.isEmpty()) {
                binding.noDataImageView.visibility = View.VISIBLE
                binding.addressesListView.adapter = AddressesAdapter(this@MapActivity, arrayListOf())
            } else {
                binding.noDataImageView.visibility = View.GONE
                binding.addressesListView.adapter = AddressesAdapter(this@MapActivity, addresses.map { it.address })
                binding.addressesListView.setOnItemClickListener { p0, p1, p2, p3 ->
                    // TODO: перемещение камеры на отметку на карте
                }
            }

            val bitmapPoint = createBitmapFromVector(R.drawable.baseline_location_pin_24)
            for (addressInfo in addresses) {
                mapObjects.addPlacemark(
                    Point(
                        addressInfo.lat.toDouble(),
                        addressInfo.lon.toDouble()
                    ),
                    ImageProvider.fromBitmap(
                        bitmapPoint,
                        true,
                        "MAP_POINT_RED"
                    ))
            }
        }
    }

    private fun createBitmapFromVector(art: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(this, art) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        ) ?: return null
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.map_optional_menu_res, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return  when (item.itemId) {
            R.id.unqueueImHereMenuItem -> {
                lifecycleScope.launch {
                    val res = getAddressByCoordsAsync(this@MapActivity, mLocation)
                    res.onSuccess { address ->
                        val serverApiAddressAdditionResponse = GeoPositionService.addAddressRecordAsync(
                            applicationContext, AddresInfo(
                                -1,
                                MainStatic.currentUser!!.id,
                                address.suggestions[0].value,
                                mLocation!!.latitude.toFloat(),
                                mLocation!!.longitude.toFloat(),
                                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toInt()
                            )
                        )
                        serverApiAddressAdditionResponse.onSuccess {
                            Toast.makeText(this@MapActivity.applicationContext, "Адрес записан", Toast.LENGTH_SHORT).show()
                        }
                        serverApiAddressAdditionResponse.onFailure { e ->
                            Log.e("MiaBox", "${e.message.toString()} | $address")
                            Toast.makeText(this@MapActivity.applicationContext, "Ошибка отправки данных на сервер", Toast.LENGTH_SHORT).show()
                        }
                    }
                    res.onFailure {
                        Log.e("MiaBox", it.message.toString())
                        Toast.makeText(this@MapActivity.applicationContext, "Ошибка запроса к стороннему API: ${it.message.toString()}", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initLocationRequest(): LocationRequest {
        var request = LocationRequest.create()
        return request.apply {
            interval = 10000
            fastestInterval = 7000
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
    }

    private val geoCallback = object : LocationCallback() {
        override fun onLocationResult(geo: LocationResult) {
            for(locationResult in geo.locations) {
                mLocation = locationResult
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onStart() {
        binding.addressesMapView.onStart()
        MapKitFactory.getInstance().onStart()
        super.onStart()
    }

    override fun onStop() {
        binding.addressesMapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onSearchResponse(response: Response) {
        val mapObjects: MapObjectCollection = binding.addressesMapView.map.mapObjects
        mapObjects.clear()

        for (searchResult in response.collection.children) {
            val resultLocation = searchResult.obj!!.geometry[0].point
            if (resultLocation != null) {
                mapObjects.addPlacemark(resultLocation, ImageProvider.fromResource(this, R.drawable.cursor_map))
            }
        }
    }

    override fun onSearchError(error: Error) {
        if (error is RemoteError) {
            Toast.makeText(this, "Ошибка сервера", Toast.LENGTH_SHORT).show()
        } else if (error is NetworkError) {
            Toast.makeText(this, "Ошибка сети", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onObjectAdded(userLocationView: UserLocationView) {
        locationMapKit.setAnchor(
            PointF((binding.addressesMapView.mapWindow.width() * 0.5).toFloat(), (binding.addressesMapView.mapWindow.height() * 0.5).toFloat()),
            PointF((binding.addressesMapView.mapWindow.width() * 0.5).toFloat(), (binding.addressesMapView.mapWindow.height() * 0.83).toFloat())
        )
        userLocationView.arrow.setIcon(ImageProvider.fromResource(this, R.drawable.cursor_arrow))
        locationMapKit.isHeadingEnabled = false
        locationMapKit.isAutoZoomEnabled = false
//        val picIcon = userLocationView.pin.useCompositeIcon()
//        picIcon.setIcon("icon",
//            ImageProvider.fromResource(this, R.drawable.cursor_map),
//            IconStyle().setAnchor(PointF(0.0f, 0.0f)).setRotationType(RotationType.NO_ROTATION).setZIndex(0.0f).setScale(1.0f))
//        picIcon.setIcon("pin",
//            ImageProvider.fromResource(this, R.drawable.cursor_map),
//            IconStyle().setAnchor(PointF(0.5f, 0.5f)).setRotationType(RotationType.NO_ROTATION).setZIndex(0.0f).setScale(0.5f))
        userLocationView.accuracyCircle.fillColor = Color.TRANSPARENT
    }

    override fun onObjectRemoved(userLocationView: UserLocationView) { }

    override fun onObjectUpdated(userLocationView: UserLocationView, objectEvent: ObjectEvent) { }

    override fun onCameraPositionChanged(
        map: Map,
        cameraPosition: CameraPosition,
        cameraUpdate: CameraUpdateReason,
        isFinished: Boolean,
    ) {
        if(isFinished) {
            if(binding.searchEditText.text.toString() == "") return

            searchSession = searchManager.submit(
                binding.searchEditText.text.toString(),
                VisibleRegionUtils.toPolygon(binding.addressesMapView.map.visibleRegion),
                SearchOptions(),
                this
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("haveApiKey", true)
    }

    companion object {

        fun getAllAddresses(userId: Int, dateStart: Int, dateEnd: Int, context: Context, callback: (ArrayList<AddresInfo>) -> Unit) {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val addressApi = retrofit.create(ServerApiAddress::class.java)

            val resp = addressApi.getUserAddressesByPeriod(userId, dateStart, dateEnd, MainStatic.currentToken!!)
            resp.enqueue(object : Callback<List<AddresInfo>> {
                override fun onResponse(call: Call<List<AddresInfo>>, response: retrofit2.Response<List<AddresInfo>>) {
                    if (response.isSuccessful) {
                        response.body()?.let { callback(ArrayList(it)) }
                        return
                    }

                    callback(arrayListOf<AddresInfo>())
                }

                override fun onFailure(call: Call<List<AddresInfo>>, t: Throwable) {
                    callback(arrayListOf<AddresInfo>())
                }
            })
        }

        suspend fun getAddressByCoordsAsync(context: Context, mLocation: Location): kotlin.Result<AddressResponse> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl("http://suggestions.dadata.ru/suggestions/api/")
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
    }
}
