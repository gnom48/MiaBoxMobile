package com.example.pronedvizapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import com.example.pronedvizapp.databinding.ActivityMapBinding
import com.example.pronedvizapp.requests.DadataApi
import com.example.pronedvizapp.requests.ServerApiUsers
import com.example.pronedvizapp.requests.models.AddressResponse
import com.example.pronedvizapp.requests.models.Coordinates
import com.example.pronedvizapp.requests.models.Statistics
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialDialogs
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapType
import com.yandex.mapkit.map.RotationType
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
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory

class MapActivity : AppCompatActivity(), Session.SearchListener, UserLocationObjectListener, CameraListener {

    lateinit var binding: ActivityMapBinding

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
        setContentView(binding.root)

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

        binding.geopositionFloatingActionButton.setOnClickListener {
            try {
                val tPoint = Point(mLocation.latitude, mLocation.longitude)

                binding.addressesMapView.map.move(
                    CameraPosition(tPoint, 12.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 2.0f), null
                )

                showAddresDialog()
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

    private fun showAddresDialog() {
        lifecycleScope.launch {
            val res = getAddressByCoords(this@MapActivity)
            res.onSuccess {
//                val dialogBuilder = AlertDialog.Builder(this@MapActivity)
//                dialogBuilder.setTitle(it.suggestions[0].value)
//                dialogBuilder.setPositiveButton("OK") { dialog, _ ->
//                    dialog.dismiss()
//                }
//                val dialog = dialogBuilder.create()
//                dialog.show()
                binding.listView.adapter = ArrayAdapter<String>(this@MapActivity, R.layout.address_card, arrayListOf(it.suggestions[0].value))
                binding.noDataImageView.visibility = View.GONE
            }
            res.onFailure {
                val e = it
                Log.e("MiaBox", e.message.toString())
            }
        }
    }

    suspend private fun getAddressByCoords(context: Context): Result<AddressResponse> = coroutineScope {
        val retrofit = Retrofit.Builder()
            .baseUrl(context.getString(R.string.server_ip_address))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val dadataApi = retrofit.create(DadataApi::class.java)

        return@coroutineScope try {
            val response = dadataApi.getAddressByCoordinates(Coordinates(mLocation.latitude, mLocation.longitude)).await()
            if (response != null) {
                Result.success(response)
            } else {
                Result.failure(Exception("Ошибка получения данных"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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

    override fun onObjectRemoved(userLocationView: UserLocationView) {

    }

    override fun onObjectUpdated(userLocationView: UserLocationView, objectEvent: ObjectEvent) {

    }

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
}