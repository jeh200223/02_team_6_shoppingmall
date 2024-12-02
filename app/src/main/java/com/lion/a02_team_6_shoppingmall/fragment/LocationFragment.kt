package com.lion.a02_team_6_shoppingmall.fragment

import android.content.Context.LOCATION_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lion.a02_team_6_shoppingmall.MainActivity
import com.lion.a02_team_6_shoppingmall.R
import com.lion.a02_team_6_shoppingmall.databinding.FragmentLocationBinding
import com.lion.a02_team_6_shoppingmall.util.FragmentName
import com.lion.a02_team_6_shoppingmall.repository.PlaceData
import com.lion.a02_team_6_shoppingmall.repository.RetrofitRequestInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LocationFragment : Fragment() {

    lateinit var fragmentLocationBinding: FragmentLocationBinding
    lateinit var mainActivity: MainActivity

    val permissionList = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )
    // 권한 확인을 위한 런처
    lateinit var permissionCheckLauncher: ActivityResultLauncher<Array<String>>

    // 위치 정보 관리 객체
    lateinit var locationManager: LocationManager
    // 위치 측정을 하면 반응하는 리스너
    lateinit var myLocationListener: MyLocationListener

    lateinit var googleMap: GoogleMap

    var userLocation:Location? = null

    var myMarker:Marker? = null
    val markerList = mutableListOf<Marker>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentLocationBinding = FragmentLocationBinding.inflate(layoutInflater)
        mainActivity = activity as MainActivity
        MapsInitializer.initialize(mainActivity, MapsInitializer.Renderer.LATEST, null)
        settingToolbar()
        createPermissionCheckLauncher()
        permissionCheckLauncher.launch(permissionList)
        settingGoogleMap()
        return fragmentLocationBinding.root
    }

    fun settingToolbar(){
        fragmentLocationBinding.materialToolbarLocation.apply {
            title = "위치기반 서비스"
            isTitleCentered = true

            setNavigationIcon(R.drawable.arrow_back_24px)
            setNavigationOnClickListener {
                mainActivity.removeFragment(FragmentName.LOCATION_FRAGMENT)
                settingFilterClear()
            }

            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.menuMyLocation -> {
                        getMyLocation()
                        getPlaceData()
                    }
                }
                true
            }
        }
    }

    fun settingFilterClear(){
        mainActivity.isLiterature = false
        mainActivity.isHumanities = false
        mainActivity.isNature = false
        mainActivity.isEtc = false
        mainActivity.isAll = false
    }

    // 권한 확인을 위해 사용할 런처 생성
    fun createPermissionCheckLauncher(){
        // 런처를 등록한다.
        permissionCheckLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            // Snackbar.make(activityMainBinding.root, "권한 확인 완료", Snackbar.LENGTH_LONG).show()
            // 모든 권한에 대해 확인한다.
            permissionList.forEach { permissionName ->
                // 현재 권한이 허용되어 있지 않다면 다이얼로그를 띄운다.
                if(it[permissionName] == false){
                    // 설정 화면을 띄우는 메서드를 호출한다.
                    startSettingActivity()
                    // 함수 종료
                    return@registerForActivityResult
                }
            }
        }
    }

    // 애플리케이션의 설정화면을 실행시키는 메서드
    fun startSettingActivity(){
        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(mainActivity)
        materialAlertDialogBuilder.setTitle("권한 확인 요청")
        materialAlertDialogBuilder.setMessage("권한을 모두 허용해줘야 정상적인 서비스 이용이 가능합니다")
        materialAlertDialogBuilder.setPositiveButton("권한 설정 하기"){ dialogInterface: DialogInterface, i: Int ->
            val uri = Uri.fromParts("package", mainActivity.packageName, null)
            val permissionIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
            permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(permissionIntent)
        }
        materialAlertDialogBuilder.show()
    }

    inner class MyLocationListener:LocationListener{
        override fun onLocationChanged(location: Location) {
            setMyLocation(location)
        }
    }

    fun getMyLocation(){
        val chk1 = ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val chk2 = ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION)

        if (chk1 == PackageManager.PERMISSION_DENIED || chk2 == PackageManager.PERMISSION_DENIED){
            permissionCheckLauncher.launch(permissionList)
            return
        }

        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, myLocationListener)
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0.0f, myLocationListener)
        }
    }

    fun setMyLocation(location: Location){
        userLocation = location

        locationManager.removeUpdates(myLocationListener)

        val loc1 = LatLng(location.latitude, location.longitude)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(loc1, 15.0f)
        googleMap.animateCamera(cameraUpdate)

        val markerOptions = MarkerOptions()
        markerOptions.position(loc1)

        if(myMarker != null){
            myMarker?.remove()
            myMarker = null
        }

        val myMarkerBitmap = BitmapDescriptorFactory.fromResource(R.drawable.person_pin)
        markerOptions.icon(myMarkerBitmap)

        myMarker = googleMap.addMarker(markerOptions)
    }

    fun settingGoogleMap(){
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync {
            googleMap = it
            locationManager = mainActivity.getSystemService(LOCATION_SERVICE) as LocationManager
            myLocationListener = MyLocationListener()

            val chk1 = ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION)
            val chk2 = ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION)

            if (chk1 == PackageManager.PERMISSION_DENIED || chk2 == PackageManager.PERMISSION_DENIED){
                permissionCheckLauncher.launch(permissionList)
                return@getMapAsync
            }

            val gpsSavedLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkSavedLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val passiveSavedLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

            if (gpsSavedLocation != null){
                setMyLocation(gpsSavedLocation)
            } else if (networkSavedLocation != null) {
                setMyLocation(networkSavedLocation)
            } else if (passiveSavedLocation != null) {
                setMyLocation(passiveSavedLocation)
            }

            getMyLocation()
            getPlaceData()
        }
    }

    fun getPlaceData(){
        if (userLocation != null){
            CoroutineScope(Dispatchers.Main).launch {
                val work1 = async(Dispatchers.IO){
                    val builder = Retrofit.Builder()
                    builder.baseUrl("https://maps.googleapis.com/")
                    builder.addConverterFactory(GsonConverterFactory.create())
                    val retrofit = builder.build()

                    val service = retrofit.create(RetrofitRequestInterface::class.java)

                    val location = "${userLocation?.latitude},${userLocation?.longitude}"
                    val radius = "1000"
                    val language = "ko"
                    val type = "book_store"
                    val key = "AIzaSyCefQseh8e6ZcSIfwclSfgCrnR0XPK9KjQ"

                    service.requestPlaceApi(location, radius, language, type, key).execute()
                }

                val resultPlace = work1.await()
                settingMarker(resultPlace.body()!!)
            }
        }
    }

    fun settingMarker(placeData: PlaceData){
        markerList.forEach {
            it.remove()
        }
        markerList.clear()

        if (placeData.status == "OK"){
            CoroutineScope(Dispatchers.Main).launch {

                placeData.results.forEach{
                    val lat = it.geometry.location.lat
                    val lng = it.geometry.location.lng
                    val placeName = it.name
                    val placeVicinity = it.vicinity

                    val markerOptions = MarkerOptions()
                    val markerLocation = LatLng(lat, lng)
                    markerOptions.position(markerLocation)
                    markerOptions.title(placeName)
                    markerOptions.snippet(placeVicinity)

                    val placeBitmap = BitmapDescriptorFactory.fromResource(R.drawable.location_on)
                    markerOptions.icon(placeBitmap)

                    val marker = googleMap.addMarker(markerOptions)
                    markerList.add(marker!!)
                }
            }
        }
    }
}